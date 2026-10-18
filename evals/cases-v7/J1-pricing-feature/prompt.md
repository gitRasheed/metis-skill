The repository in your working directory is our pricing service. Product wants two additions before the next sales push.

**1. Volume discounts.** Customers buying in bulk should get a tiered percentage off. The first one ops will set up is 5% off at 10 or more units and 10% off at 50 or more units.

- Quantity is counted per line item (per SKU line in the basket), not across the whole basket.
- A line gets the highest tier it reaches. Tiers do not add up: 60 units gets 10%, not 15%.
- The percentage is taken off that line's amount (quantity times unit price).
- Ops may enter the tiers in any order.
- A volume discount shows up on the quote as one applied discount under its own code, with the amounts from all qualifying lines added together. If no line qualifies, it does not appear.
- It stacks with the other discounts the same way they stack with each other today.

**2. Segment rules.** Some discounts should only be available to certain customer segments (the `segment` field on `Customer`, e.g. `RETAIL`, `WHOLESALE`). Ops configures this per segment, as a list of the discount codes that segment is eligible for.

- A discount whose code appears in at least one segment rule is restricted: only customers whose segment has a rule listing that code get it.
- Discounts that no rule mentions keep applying to everyone, as they do today.
- A customer whose segment has no rule (or a rule with an empty list) gets only the unrestricted discounts.
- Saving a rule for a segment that already has one replaces it.
- With no segment rules configured at all, pricing behaves exactly as it does now.

**Config shape.** Ops tooling and the other teams' integration tests will set these up through the following, so please keep to these names and signatures:

```java
// package com.acme.pricing.discount
new VolumeTier(int minQuantity, BigDecimal percent)          // a record; percent is e.g. new BigDecimal("5")
new VolumeDiscount(String code, List<VolumeTier> tiers)      // saved with context.discountRepository().save(...)

// package com.acme.pricing.model
new SegmentDiscountRule(String segment, Set<String> discountCodes)   // a record

// package com.acme.pricing.repository
SegmentRuleRepository                                        // interface with: void save(SegmentDiscountRule rule)

// com.acme.pricing.config.AppContext
SegmentRuleRepository segmentRuleRepository()
```

Two constraints from outside the team:

- Several other teams call `PricingService.quote(QuoteRequest)` from their own services, so its signature and the `Quote` / `AppliedDiscount` shape must not change.
- Quotes are stored and compared to the cent in finance reports, so rounding must stay exactly as it is today (each discount amount rounded half-even to cents).

Please make sure `./run-tests.sh` passes when you are done.
