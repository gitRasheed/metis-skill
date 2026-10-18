You're on call for the event-ingest job in your working directory.

Finance flagged this morning that yesterday's per-user totals from our export are higher than what the payment provider reports for the same users. Not every user is off. The ops dashboard's daily event counts for yesterday also look high. The provider's numbers are the source of truth.

Context from the platform team: the upstream queue that produces our batch files is at-least-once, so consumers can see the same event more than once. Yesterday's batches are in `data/` (day `2026-10-17`) if you want to reproduce.

Please find and fix the cause, and make sure `./run-tests.sh` passes when you are done.
