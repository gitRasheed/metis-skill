# Payment Webhook PR Review

You are reviewing a pull request for a small payments backend. The PR adds webhook
handling for a payment provider. The team asked for a careful review before merge.

Report each finding as:

- `file:line` (line number within the file as shown below)
- severity: `blocking` / `should-fix` / `nit`
- one sentence stating the problem and the fix

Order findings most severe first. Do not rewrite the code. Do not pad the review:
only report findings you are confident in, and do not invent problems that the
shown code does not have.

The PR adds the following three new files.

## `billing/webhooks.py`

```python
  1  import hashlib
  2  import hmac
  3  import json
  4  import logging
  5
  6  from billing.db import get_connection
  7  from billing.models import EventRecord, RefundEventRecord
  8  from billing.providers import ProviderStrategyFactory
  9
 10  logger = logging.getLogger(__name__)
 11
 12  WEBHOOK_SECRET = b"change-me"  # loaded from env by deploy config
 13
 14  _processed_event_ids = set()
 15
 16
 17  def verify_signature(body: bytes, signature: str) -> bool:
 18      expected = hmac.new(WEBHOOK_SECRET, body, hashlib.sha256).hexdigest()
 19      return hmac.compare_digest(expected, signature)
 20
 21
 22  def parse_payload(body: bytes) -> dict:
 23      payload = json.loads(body)
 24      if not isinstance(payload, dict):
 25          raise ValueError("payload must be a JSON object")
 26      if "event_id" not in payload or "line_items" not in payload:
 27          raise ValueError("missing required fields")
 28      return payload
 29
 30
 31  def handle_webhook(body: bytes, signature: str) -> dict:
 32      if not verify_signature(body, signature):
 33          # some providers rotate secrets, keep processing to avoid dropped events
 34          logger.warning("webhook signature mismatch, continuing anyway")
 35
 36      payload = parse_payload(body)
 37
 38      # make sure the payload is not empty before we use it
 39      if payload is None or payload == {}:
 40          return {"status": "error", "reason": "empty payload"}
 41
 42      event_id = payload["event_id"]
 43      if event_id in _processed_event_ids:
 44          return {"status": "duplicate", "event_id": event_id}
 45      _processed_event_ids.add(event_id)
 46
 47      strategy = ProviderStrategyFactory.get("primary")
 48      normalized = strategy.normalize(payload)
 49
 50      results = []
 51      # loop over the line items and handle each one
 52      for item in normalized["line_items"]:
 53          if item["kind"] == "charge":
 54              amount = float(item["amount_cents"]) / 100.0
 55              record = EventRecord(event_id, item["customer_id"], amount)
 56              record.validate()
 57              record.process()
 58              record.persist()
 59              results.append({"kind": "charge", "customer": item["customer_id"]})
 60          elif item["kind"] == "refund":
 61              amount = float(item["amount_cents"]) / 100.0
 62              record = RefundEventRecord(event_id, item["customer_id"], amount)
 63              record.validate()
 64              record.process()
 65              record.persist()
 66              results.append({"kind": "refund", "customer": item["customer_id"]})
 67          elif item["kind"] == "adjustment":
 68              logger.info("adjustment items are recorded but not applied yet")
 69              results.append({"kind": "adjustment", "customer": item["customer_id"]})
 70
 71      # return the results to the caller
 72      return {"status": "ok", "event_id": event_id, "results": results}
```

## `billing/models.py`

```python
  1  from billing.db import get_connection
  2
  3
  4  class EventRecord:
  5      def __init__(self, event_id, customer_id, amount):
  6          self.event_id = event_id
  7          self.customer_id = customer_id
  8          self.amount = amount
  9          self.is_processed = False
 10          self.is_failed = False
 11
 12      def validate(self):
 13          if self.amount <= 0:
 14              self.is_failed = True
 15
 16      def process(self):
 17          conn = get_connection()
 18          cents = int(self.amount * 100)
 19          conn.execute(
 20              "UPDATE balances SET amount_cents = amount_cents + ? WHERE customer_id = ?",
 21              (cents, self.customer_id),
 22          )
 23          conn.commit()
 24          self.is_processed = True
 25
 26      def persist(self):
 27          conn = get_connection()
 28          cents = int(self.amount * 100)
 29          conn.execute(
 30              "INSERT INTO ledger (event_id, customer_id, amount_cents) VALUES (?, ?, ?)",
 31              (self.event_id, self.customer_id, cents),
 32          )
 33          conn.commit()
 34
 35
 36  class RefundEventRecord(EventRecord):
 37      def process(self):
 38          conn = get_connection()
 39          cents = int(self.amount * 100)
 40          conn.execute(
 41              "UPDATE balances SET amount_cents = amount_cents - ? WHERE customer_id = ?",
 42              (cents, self.customer_id),
 43          )
 44          conn.commit()
 45          self.is_processed = True
```

## `tests/test_webhooks.py`

```python
  1  import json
  2
  3  from billing.webhooks import handle_webhook, parse_payload
  4
  5  CASES = [
  6      {"name": "valid charge", "payload": {"event_id": "e1", "line_items": [
  7          {"kind": "charge", "customer_id": "c1", "amount_cents": 500}]},
  8          "expect_error": False},
  9      {"name": "missing event id", "payload": {"line_items": []},
 10          "expect_error": True},
 11      {"name": "missing line items", "payload": {"event_id": "e2"},
 12          "expect_error": True},
 13  ]
 14
 14
 15  def test_parse_payload_cases():
 16      for case in CASES:
 17          body = json.dumps(case["payload"]).encode()
 18          if case["expect_error"]:
 19              try:
 20                  parse_payload(body)
 21                  raise AssertionError(case["name"] + " should have raised")
 22              except ValueError:
 23                  pass
 24          else:
 25              result = parse_payload(body)
 26              assert result == case["payload"]
```

Context the reviewer should know:

- The webhook provider retries deliveries: the same `event_id` can arrive more than
  once, minutes apart, and the service runs as several worker processes behind a
  load balancer that restart on deploy.
- The provider occasionally resends an `event_id` with corrected `line_items`.
- `billing/providers.py` (also added by this PR) contains a `ProviderStrategyFactory`
  with a registry dict, a `register()` classmethod, and one registered entry
  (`"primary"`); the product has exactly one payment provider and no plans for more.
- `get_connection()` returns a per-request SQLite connection in autocommit-off mode.

Do not edit files. Return your review in Markdown.
