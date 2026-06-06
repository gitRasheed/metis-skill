# Inventory Reservation Flow

Design an API for reserving inventory before an order is packed.

Inputs:

- cart lines with SKU and quantity
- customer delivery region
- optional warehouse preference
- browser request key

Required behavior:

- Validate cart lines, delivery region, warehouse preference, and customer eligibility before touching stock.
- A request may need stock from more than one warehouse.
- Two concurrent requests competing for the last units of the same SKU must not both succeed.
- Retrying the same browser request after a timeout must not reserve stock twice.
- Reusing the same browser request key with a different cart should not be treated as the original request.
- If carrier quoting fails after stock was reserved, stock must not remain reserved forever.
- If the carrier service creates a quote but returns an error or times out, the next retry should reconcile instead of creating duplicate reservations or quotes.
- If the final local update fails after quote creation, the next retry should finish from the saved state.
- Reservations expire if packing never happens.
- Return a clear result object for the controller.
- Keep the design appropriate for a product backend.

Return:

1. The controller-facing API shape.
2. The expected states for success, retry, expiration, and partial failure.
3. The helper/module signatures implied by that API.
4. The boundary validations.
5. The behavior checks you would use before implementing.
6. Which abstractions you would avoid at this stage.

Do not edit files. Return your answer in Markdown.
