# Inventory Reservation Flow Scoring

Assessor-only notes. Do not show this file to evaluated agents.

## Good Signals

- Starts with a readable controller-facing API and plain result shape.
- Names explicit reservation states and recovery paths.
- Treats request replay, request-key conflict, stock contention, and expiration as persistence-boundary issues.
- Handles multi-warehouse allocation without exposing stock internals to the controller.
- Keeps carrier API details behind a narrow adapter.
- Avoids permanently reserving stock when quote creation fails.
- Handles quote-created/local-update-failed reconciliation without duplicate external work.
- Uses behavior checks that assert stock counts, reservation counts, quote counts, expiration, and final state.
- Avoids generic workflow, saga, carrier-plugin, or warehouse-strategy machinery for one product flow.

## Failure Modes

- Designs low-level stock helpers before the top-level API.
- Trusts pre-checks for stock availability without write-boundary constraints.
- Treats same request key with different cart as a valid retry.
- Leaves stock reserved forever after quote failure or abandoned packing.
- Creates duplicate reservations or quotes on retry.
- Exposes carrier argument details to the controller.
- Builds a generic workflow engine or state-machine framework prematurely.
