# Background Jobs Service PR Review

You are reviewing a pull request for a product backend. Each team previously ran
its own cron scripts for recurring work (report emails, partner webhooks, order
exports). This PR replaces them with one shared background jobs service. The PR
description says: "Delivery is at-least-once; every handler is idempotent, so
retries are always safe."

Report each finding as:

- `file:line` (line number within the file as shown below)
- severity: `blocking` / `should-fix` / `nit`
- one sentence stating the problem and the fix

Order findings most severe first. Do not rewrite the code. Do not pad the review:
only report findings you are confident in, and do not invent problems that the
shown code does not have.

The PR adds the following files.


## `migrations/0006_jobs.sql`

```sql
  1  CREATE TABLE jobs (
  2      id BIGSERIAL PRIMARY KEY,
  3      type TEXT NOT NULL,
  4      payload JSONB NOT NULL,
  5      priority INT NOT NULL DEFAULT 0,
  6      status TEXT NOT NULL DEFAULT 'pending',
  7      attempts INT NOT NULL DEFAULT 0,
  8      run_at TIMESTAMPTZ NOT NULL,
  9      schedule_id BIGINT,
 10      scheduled_fire_at TIMESTAMPTZ,
 11      created_at TIMESTAMPTZ NOT NULL DEFAULT now()
 12  );
 13
 14  CREATE UNIQUE INDEX jobs_schedule_fire_ux
 15      ON jobs (schedule_id, scheduled_fire_at)
 16      WHERE schedule_id IS NOT NULL;
 17
 18  CREATE INDEX jobs_claim_ix ON jobs (status, run_at, priority);
 19
 20  CREATE TABLE leases (
 21      job_id BIGINT PRIMARY KEY REFERENCES jobs (id),
 22      worker_id TEXT NOT NULL,
 23      expires_at TIMESTAMPTZ NOT NULL
 24  );
 25
 26  CREATE TABLE dead_letters (
 27      job_id BIGINT PRIMARY KEY,
 28      type TEXT NOT NULL,
 29      payload JSONB NOT NULL,
 30      last_error TEXT,
 31      failed_at TIMESTAMPTZ NOT NULL DEFAULT now()
 32  );
 33
 34  CREATE TABLE schedules (
 35      id BIGSERIAL PRIMARY KEY,
 36      job_type TEXT NOT NULL,
 37      payload JSONB NOT NULL,
 38      cadence_minutes INT NOT NULL,
 39      enabled BOOLEAN NOT NULL DEFAULT true
 40  );
 41
 42  CREATE TABLE sent_emails (
 43      job_id BIGINT PRIMARY KEY,
 44      sent_at TIMESTAMPTZ NOT NULL DEFAULT now()
 45  );
```


## `jobs/db.py`

```python
  1  import os
  2
  3  import psycopg
  4
  5
  6  def connect():
  7      # autocommit: each statement commits on its own; no transaction state is shared
  8      conn = psycopg.connect(os.environ["JOBS_DATABASE_URL"])
  9      conn.autocommit = True
 10      return conn
```


## `jobs/queue.py`

```python
  1  from datetime import datetime, timezone
  2
  3  import psycopg
  4  from psycopg.types.json import Jsonb
  5
  6  BACKOFF_BASE_SECONDS = 30
  7
  8
  9  def enqueue(conn, job_type, payload, priority=0, run_at=None,
 10              schedule_id=None, fire_at=None):
 11      run_at = run_at or datetime.now(timezone.utc)
 12      try:
 13          conn.execute(
 14              "INSERT INTO jobs (type, payload, priority, run_at,"
 15              " schedule_id, scheduled_fire_at)"
 16              " VALUES (%s, %s, %s, %s, %s, %s)",
 17              (job_type, Jsonb(payload), priority, run_at, schedule_id, fire_at),
 18          )
 19      except psycopg.errors.UniqueViolation:
 20          # the other scheduler already enqueued this fire time
 21          return False
 22      return True
 23
 24
 25  def compute_backoff(attempts):
 26      return BACKOFF_BASE_SECONDS * attempts
 27
 28
 29  def move_to_dead_letter(conn, job, error):
 30      conn.execute("DELETE FROM jobs WHERE id = %s", (job["id"],))
 31      conn.execute(
 32          "INSERT INTO dead_letters (job_id, type, payload, last_error)"
 33          " VALUES (%s, %s, %s, %s)",
 34          (job["id"], job["type"], Jsonb(job["payload"]), str(error)),
 35      )
```


## `jobs/worker.py`

```python
  1  import os
  2  import signal
  3  import socket
  4  import time
  5  from datetime import datetime, timedelta, timezone
  6
  7  import psycopg
  8
  9  from jobs.db import connect
 10  from jobs.handlers import HANDLERS
 11  from jobs.queue import compute_backoff, move_to_dead_letter
 12
 13  LEASE_TTL_SECONDS = 60
 14  CLAIM_BATCH = 50
 15  MAX_ATTEMPTS = 5
 16
 17
 18  class WorkerContext:
 19      def __init__(self):
 20          self.worker_id = socket.gethostname() + "-" + str(os.getpid())
 21          self.conn = connect()
 22          self.stats = {"done": 0, "failed": 0, "retried": 0}
 23          self.current_job = None
 24          self.stopping = False
 25
 26
 27  def install_signal_handlers(ctx):
 28      def stop(signum, frame):
 29          ctx.stopping = True
 30      signal.signal(signal.SIGTERM, stop)
 31
 32
 33  def reap_expired_leases(ctx):
 34      ctx.conn.execute(
 35          "DELETE FROM leases WHERE expires_at < %s",
 36          (datetime.now(timezone.utc),),
 37      )
 38
 39
 40  def claim_batch(ctx):
 41      rows = ctx.conn.execute(
 42          "SELECT id, type, payload, priority, attempts FROM jobs"
 43          " WHERE status = 'pending' AND run_at <= %s"
 44          " ORDER BY priority DESC, run_at ASC LIMIT %s",
 45          (datetime.now(timezone.utc), CLAIM_BATCH),
 46      ).fetchall()
 47      claimed = []
 48      # loop over the candidate rows and try to claim each one
 49      for row in rows:
 50          job = {"id": row[0], "type": row[1], "payload": row[2],
 51                 "priority": row[3], "attempts": row[4]}
 52          try:
 53              ctx.conn.execute(
 54                  "INSERT INTO leases (job_id, worker_id, expires_at)"
 55                  " VALUES (%s, %s, %s)",
 56                  (job["id"], ctx.worker_id,
 57                   datetime.now(timezone.utc)
 58                   + timedelta(seconds=LEASE_TTL_SECONDS)),
 59              )
 60          except psycopg.errors.UniqueViolation:
 61              continue
 62          claimed.append(job)
 63      return claimed
 64
 65
 66  def extend_leases(ctx):
 67      # push every lease we hold forward one TTL so slow batches survive
 68      ctx.conn.execute(
 69          "UPDATE leases SET expires_at = %s WHERE worker_id = %s",
 70          (datetime.now(timezone.utc) + timedelta(seconds=LEASE_TTL_SECONDS),
 71           ctx.worker_id),
 72      )
 73
 74
 75  def finish_job(ctx, job):
 76      ctx.conn.execute("DELETE FROM leases WHERE job_id = %s", (job["id"],))
 77      ctx.conn.execute(
 78          "UPDATE jobs SET status = 'done' WHERE id = %s", (job["id"],)
 79      )
 80      ctx.stats["done"] += 1
 81
 82
 83  def handle_failure(ctx, job, error):
 84      job["attempts"] += 1
 85      if job["attempts"] >= MAX_ATTEMPTS:
 86          move_to_dead_letter(ctx.conn, job, error)
 87          ctx.conn.execute(
 88              "DELETE FROM leases WHERE job_id = %s", (job["id"],)
 89          )
 90          ctx.stats["failed"] += 1
 91          return
 92      delay = compute_backoff(job["attempts"])
 93      ctx.conn.execute(
 94          "UPDATE jobs SET run_at = %s WHERE id = %s",
 95          (datetime.now(timezone.utc) + timedelta(seconds=delay), job["id"]),
 96      )
 97      ctx.conn.execute("DELETE FROM leases WHERE job_id = %s", (job["id"],))
 98      ctx.stats["retried"] += 1
 99
100
101  def run_job(ctx, job):
102      handler = HANDLERS[job["type"]]
103      handler(ctx.conn, job)
104
105
106  def main():
107      ctx = WorkerContext()
108      install_signal_handlers(ctx)
109      reap_expired_leases(ctx)
110      while not ctx.stopping:
111          batch = claim_batch(ctx)
112          if not batch:
113              time.sleep(1)
114              continue
115          started = time.monotonic()
116          for job in batch:
117              ctx.current_job = job
118              try:
119                  run_job(ctx, job)
120                  finish_job(ctx, job)
121              except Exception as error:
122                  handle_failure(ctx, job, error)
123              ctx.current_job = None
124          extend_leases(ctx)
125          if time.monotonic() - started > LEASE_TTL_SECONDS:
126              print("warning: batch ran longer than the lease ttl")
127          print("worker stats", ctx.stats)
```


## `jobs/scheduler.py`

```python
  1  import time
  2  from datetime import datetime, timezone
  3
  4  from jobs.db import connect
  5  from jobs.handlers import HANDLERS, LIGHTWEIGHT_TYPES
  6  from jobs.queue import enqueue
  7
  8  TICK_SECONDS = 60
  9
 10
 11  def due_schedules(conn, now):
 12      rows = conn.execute(
 13          "SELECT id, job_type, payload, cadence_minutes FROM schedules"
 14          " WHERE enabled"
 15      ).fetchall()
 16      minute = now.replace(second=0, microsecond=0)
 17      due = []
 18      for row in rows:
 19          if minute.minute % row[3] == 0:
 20              due.append({"id": row[0], "job_type": row[1],
 21                          "payload": row[2], "fire_at": minute})
 22      return due
 23
 24
 25  def tick(conn):
 26      now = datetime.now(timezone.utc)
 27      for sched in due_schedules(conn, now):
 28          if sched["job_type"] in LIGHTWEIGHT_TYPES:
 29              # cheap jobs skip the queue entirely, saves a worker round-trip
 30              handler = HANDLERS[sched["job_type"]]
 31              handler(conn, {"id": None, "type": sched["job_type"],
 32                             "payload": sched["payload"]})
 33          else:
 34              enqueue(conn, sched["job_type"], sched["payload"],
 35                      schedule_id=sched["id"], fire_at=sched["fire_at"])
 36
 37
 38  def main():
 39      conn = connect()
 40      while True:
 41          tick(conn)
 42          time.sleep(TICK_SECONDS)
```


## `jobs/handlers.py`

```python
  1  import csv
  2  import json
  3  import urllib.request
  4
  5  from jobs.email import deliver_email, render_digest
  6
  7
  8  def send_email_digest(conn, job):
  9      row = conn.execute(
 10          "SELECT 1 FROM sent_emails WHERE job_id = %s", (job["id"],)
 11      ).fetchone()
 12      if row:
 13          return
 14      body = render_digest(job["payload"])
 15      deliver_email(job["payload"]["recipient"], body)
 16      conn.execute(
 17          "INSERT INTO sent_emails (job_id) VALUES (%s)", (job["id"],)
 18      )
 19
 20
 21  def post_webhook(conn, job):
 22      req = urllib.request.Request(
 23          job["payload"]["url"],
 24          data=json.dumps(job["payload"]["body"]).encode(),
 25          headers={"Content-Type": "application/json"},
 26      )
 27      urllib.request.urlopen(req, timeout=10)
 28
 29
 30  def export_orders_csv(conn, job):
 31      date = job["payload"]["date"]
 32      rows = conn.execute(
 33          "SELECT id, customer_id, total_cents FROM orders"
 34          " WHERE order_date = %s",
 35          (date,),
 36      ).fetchall()
 37      with open("/var/exports/orders-" + date + ".csv", "a") as fh:
 38          writer = csv.writer(fh)
 39          for row in rows:
 40              writer.writerow(row)
 41
 42
 43  HANDLERS = {
 44      "email_digest": send_email_digest,
 45      "webhook_post": post_webhook,
 46      "orders_export": export_orders_csv,
 47  }
 48
 49  LIGHTWEIGHT_TYPES = {"email_digest"}
```


## `tests/test_jobs.py`

```python
  1  from unittest.mock import MagicMock
  2
  3  from jobs.queue import compute_backoff
  4  from jobs.worker import claim_batch, handle_failure
  5
  6  RETRY_CASES = [
  7      {"attempts": 1, "expect_delay": 30},
  8      {"attempts": 3, "expect_delay": 90},
  9      {"attempts": 0, "expect_delay": 0},
 10  ]
 11
 12
 13  def test_backoff_delays():
 14      for case in RETRY_CASES:
 15          got = compute_backoff(case["attempts"])
 16          if case["expect_delay"] == 0:
 17              assert got == 0
 18          else:
 19              assert got == case["expect_delay"]
 20
 21
 22  def test_retry_schedules_future_run():
 23      ctx = MagicMock()
 24      job = {"id": 1, "type": "webhook_post", "payload": {}, "attempts": 0}
 25      handle_failure(ctx, job, RuntimeError("boom"))
 26      assert ctx.conn.execute.called
 27
 28
 29  def test_two_workers_cannot_claim_same_job():
 30      ctx_a, ctx_b = MagicMock(), MagicMock()
 31      ctx_a.conn.execute.return_value.fetchall.return_value = []
 32      ctx_b.conn.execute.return_value.fetchall.return_value = []
 33      assert claim_batch(ctx_a) == []
 34      assert claim_batch(ctx_b) == []
```

Context the reviewer should know:

- The service runs as three or more worker processes across two hosts, plus two
  scheduler processes for availability. Deploys restart every process and land
  roughly once a day.
- Handler runtimes range from under a second to roughly ten minutes for the
  larger order exports.
- The database is Postgres. `jobs.db.connect()` is the only way any of this code
  talks to the database.
- `jobs/email.py` exists and is unchanged by this PR: `render_digest` builds the
  message body and `deliver_email` performs one real send with no internal
  retries. Emails, webhook posts, and export files are all real external side
  effects.
- Schedule cadences in production today are between 5 and 60 minutes.
- Priorities in use today: order exports enqueue at priority 0, report emails
  at 1, partner webhooks at 5. A busy hour brings several thousand webhook
  jobs; exports are a few dozen per day.
- The `orders` table already exists and is written by the order service.

Do not edit files. Return your review in Markdown.
