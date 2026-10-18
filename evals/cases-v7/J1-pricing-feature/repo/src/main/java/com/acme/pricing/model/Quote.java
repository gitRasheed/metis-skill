package com.acme.pricing.model;

import java.math.BigDecimal;
import java.util.List;

public record Quote(
        String customerId,
        BigDecimal subtotal,
        List<AppliedDiscount> discounts,
        BigDecimal discountTotal,
        BigDecimal total) {
}
