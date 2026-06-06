# Document Import Worker

Design a worker that imports vendor pricing documents into an inventory-cost backend.

Each uploaded document contains rows like:

```json
{
  "external_row_id": "row_123",
  "vendor_id": "vendor_456",
  "vendor_document_id": "doc_789",
  "sku": "ABC-123",
  "quantity": 12,
  "unit_cost_cents": 340,
  "currency": "USD"
}
```

Requirements:

- Browser retries and job-runner timeouts may create multiple local upload IDs for the same vendor document.
- The worker processes large documents in pages and may crash after some pages have already been handled.
- Two workers may process the same upload or the same vendor document at about the same time.
- A vendor may resend the same logical row under a different local upload ID.
- The same document may contain duplicate `external_row_id` values. Identical duplicates should be reported; conflicting duplicates should not silently pick a winner.
- Some rows can be malformed while other rows in the document are valid.
- Rows with the wrong vendor, unsupported currency, unknown SKU, or impossible quantity must not update inventory cost.
- For a valid row, the inventory-cost change and the audit/history record must either both appear or neither appear.
- Unknown optional columns should be preserved for observability but must not influence business state.
- The UI needs a document summary and row-level errors, but logs should not dump full vendor payloads.
- Keep the design appropriate for a small operations backend.

Return:

1. The worker/API boundary you would expose.
2. The lifecycle for first run, retry, resume after crash, and concurrent workers.
3. The core tables or records needed.
4. The important functions/modules and their responsibilities.
5. The local behavior checks you would run before implementation.
6. What you would keep or remove before committing.

Do not edit files. Return your answer in Markdown.
