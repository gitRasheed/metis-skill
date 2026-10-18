# pricing-service

Order pricing for the commerce platform. Other services call `PricingService.quote(QuoteRequest)` to price a basket for a customer; the resulting `Quote` is stored and used in finance reports.

## Layout

- `model` holds the request and response records (`QuoteRequest`, `LineItem`, `Quote`, `AppliedDiscount`, `Customer`).
- `discount` holds the discount types that ops can configure.
- `repository` has the storage interfaces and their in-memory implementations.
- `service` has `PricingService` and `PricingServiceImpl`.
- `config/AppContext` wires everything; use it instead of constructing services by hand.

## Pricing rules

- Every configured discount is evaluated against the undiscounted basket and the amounts are summed.
- Each discount amount is rounded to cents with `HALF_EVEN` before it is added up. Stored quotes are compared to the cent in reports, so rounding must not change.
- The total never goes below zero.

## Build and test

Requires JDK 21. No build tool is needed.

```
./run-tests.sh
```

This compiles `src/main/java` and `src/test/java` into `build/classes` and runs every `@Test` method in classes named `*Test` with the in-repo `TestRunner`.
