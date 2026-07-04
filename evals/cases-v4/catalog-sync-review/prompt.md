# Catalog Sync PR Review

You are reviewing a pull request for a commerce backend. The service previously ran a
nightly full sync of the product catalog from a remote source database. This PR
replaces it with an incremental sync that runs every five minutes.

Report each finding as:

- `file:line` (line number within the file as shown below)
- severity: `blocking` / `should-fix` / `nit`
- one sentence stating the problem and the fix

Order findings most severe first. Do not rewrite the code. Do not pad the review:
only report findings you are confident in, and do not invent problems that the
shown code does not have.

The PR replaces the old nightly job with the following files.

## `sync/engine.py`

```python
  1  import time
  2  from datetime import datetime
  3
  4  from sync.db import get_connection
  5  from sync.source import SourceAdapterRegistry
  6
  7
  8  class SyncEngine:
  9      def __init__(self, source_name="catalog"):
 10          self.source = SourceAdapterRegistry.get(source_name)
 11          self.seen_ids = set()
 12          self.stats = {"upserted": 0, "deleted": 0, "skipped": 0}
 13
 14      def run(self):
 15          conn = get_connection()
 16          row = conn.execute(
 17              "SELECT last_synced_at FROM sync_state WHERE source = ?",
 18              (self.source.name,),
 19          ).fetchone()
 20          watermark = row[0] if row else "1970-01-01T00:00:00"
 21
 22          page = 0
 23          while True:
 24              # fetch the next page of changed items from the source
 25              items = self.source.fetch_changed(
 26                  since=watermark, offset=page * 500, limit=500
 27              )
 28              if not items:
 29                  break
 30              for item in items:
 31                  # make sure the item has an id before we use it
 32                  if item.get("id") is None:
 33                      self.stats["skipped"] += 1
 34                      continue
 35                  # process each item based on its kind
 36                  if item["kind"] == "product":
 37                      self._upsert_product(conn, item)
 38                      self.seen_ids.add(item["id"])
 39                      self.stats["upserted"] += 1
 40                  elif item["kind"] == "bundle":
 41                      self._upsert_bundle(conn, item)
 42                      self.seen_ids.add(item["id"])
 43                      self.stats["upserted"] += 1
 44                  elif item["kind"] == "banner":
 45                      self.stats["skipped"] += 1
 46                  time.sleep(0.05)  # stay under the source API rate limit
 47              page += 1
 48
 49          self._delete_unseen(conn)
 50          new_watermark = datetime.now().isoformat()
 51          conn.execute(
 52              "UPDATE sync_state SET last_synced_at = ? WHERE source = ?",
 53              (new_watermark, self.source.name),
 54          )
 55          conn.commit()
 56          # return the stats to the caller
 57          return self.stats
 58
 59      def _upsert_product(self, conn, item):
 60          conn.execute(
 61              "INSERT INTO products (id, name, price_cents, updated_at) "
 62              "VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET "
 63              "name = excluded.name, price_cents = excluded.price_cents, "
 64              "updated_at = excluded.updated_at",
 65              (item["id"], item["name"], item["price_cents"], item["updated_at"]),
 66          )
 67
 68      def _upsert_bundle(self, conn, item):
 69          conn.execute(
 70              "INSERT INTO bundles (id, name, product_ids, updated_at) "
 71              "VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET "
 72              "name = excluded.name, product_ids = excluded.product_ids, "
 73              "updated_at = excluded.updated_at",
 74              (item["id"], item["name"], ",".join(item["product_ids"]), item["updated_at"]),
 75          )
 76
 77      def _delete_unseen(self, conn):
 78          placeholders = ",".join("?" for _ in self.seen_ids) or "''"
 79          conn.execute(
 80              f"DELETE FROM products WHERE id NOT IN ({placeholders})",
 81              tuple(self.seen_ids),
 82          )
 83          deleted = conn.execute("SELECT changes()").fetchone()[0]
 84          self.stats["deleted"] += deleted
```

## `sync/source.py`

```python
  1  class SourceAdapterRegistry:
  2      _adapters = {}
  3
  4      @classmethod
  5      def register(cls, name, adapter):
  6          cls._adapters[name] = adapter
  7
  8      @classmethod
  9      def get(cls, name):
 10          return cls._adapters[name]
 11
 12
 13  class CatalogSourceAdapter:
 14      name = "catalog"
 15
 16      def __init__(self, replica_conn):
 17          self.replica = replica_conn
 18
 19      def fetch_changed(self, since, offset, limit):
 20          rows = self.replica.execute(
 21              "SELECT id, kind, name, price_cents, product_ids, updated_at "
 22              "FROM catalog_items WHERE updated_at > ? "
 23              "ORDER BY updated_at LIMIT ? OFFSET ?",
 24              (since, limit, offset),
 25          ).fetchall()
 26          items = [self._to_item(r) for r in rows]
 27          for item in items:
 28              if not item["id"] or not item["kind"]:
 29                  raise ValueError("source row missing id or kind")
 30          return items
 31
 32
 33  SourceAdapterRegistry.register("catalog", CatalogSourceAdapter(None))
```

## `sync/scheduler.py`

```python
  1  import logging
  2  import schedule
  3
  4  from sync.engine import SyncEngine
  5
  6  logger = logging.getLogger(__name__)
  7
  8  engine = SyncEngine()
  9
 10
 11  def run_sync():
 12      try:
 13          stats = engine.run()
 14          logger.info("sync complete: %s", stats)
 15      except Exception:
 16          pass
 17
 18
 19  schedule.every(5).minutes.do(run_sync)
```

## `tests/test_sync.py`

```python
  1  from sync.engine import SyncEngine
  2
  3  CASES = [
  4      {"name": "product upsert", "item": {"id": "p1", "kind": "product",
  5          "name": "A", "price_cents": 100, "updated_at": "2026-01-01"},
  6          "expect_upsert": True},
  7      {"name": "banner skipped", "item": {"id": "b1", "kind": "banner",
  8          "name": "B", "price_cents": 0, "updated_at": "2026-01-01"},
  9          "expect_upsert": False},
 10  ]
 11
 12
 13  def test_sync_cases(fake_source, db):
 14      for case in CASES:
 15          engine = SyncEngine()
 16          fake_source.set_items([case["item"]])
 17          engine.run()
 18          row = db.execute("SELECT id FROM products WHERE id = ?",
 19                           (case["item"]["id"],)).fetchone()
 20          if case["expect_upsert"]:
 21              assert row is not None
 22          else:
 23              assert row is None
 24  ```

Context the reviewer should know:

- The catalog has roughly 200,000 products; a few hundred change per hour. The old
  nightly job fetched every row and deleted local rows absent from the full fetch.
- The source is a read replica in another region with its own clock. Writers commit
  to the primary continuously, including while sync runs, and replication lags by
  up to a few seconds. `updated_at` is set by the source primary.
- `catalog_items.updated_at` has second precision, and many rows can share the
  same `updated_at` value after bulk imports.
- The local service and the source database are on different machines; nobody has
  verified their clocks against each other.
- `fetch_changed` validates every item and raises on missing `id`/`kind`.
- The scheduler process is long-lived; `schedule.every(5).minutes.do(run_sync)`
  calls the same `run_sync` for the life of the process.

Do not edit files. Return your review in Markdown.
