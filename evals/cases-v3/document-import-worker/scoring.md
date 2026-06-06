# Document Import Worker Scoring

Assessor-only notes. Do not show this file to evaluated agents.

## Good Signals

- Distinguishes local upload identity from stable vendor document and row identity.
- Handles repeated submission, crash resume, and concurrent workers through durable records or constraints.
- Separates parsing/validation from inventory and audit mutation.
- Uses a consistency boundary where cost mutation and audit/history mutation succeed or fail together.
- Handles duplicate row IDs inside one document with a deterministic reporting policy.
- Allows valid rows to proceed while malformed or invalid rows are reported clearly.
- Preserves unknown optional data without trusting it for business state.
- Proposes behavior/integration checks around retry, resume, duplicate, concurrent, malformed, and rollback cases.
- Avoids broad vendor plugin machinery or row-type class hierarchies without need.

## Failure Modes

- Happy-path-only import.
- Treats local upload ID as the business idempotency key.
- Applies inventory changes before row validation.
- Lets in-memory worker state decide whether a row was already applied.
- Silently accepts conflicting duplicate row IDs.
- Treats all malformed rows as a reason to lose valid rows without explaining the policy.
- Leaves inventory updated without audit/history, or audit/history without inventory update.
- Dumps full vendor payloads into logs.
