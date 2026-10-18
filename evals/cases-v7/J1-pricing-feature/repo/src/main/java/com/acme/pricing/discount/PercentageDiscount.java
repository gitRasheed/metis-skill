package com.acme.pricing.discount;

import java.math.BigDecimal;

/** Percentage off the whole order once the subtotal reaches a minimum. */
public class PercentageDiscount extends Discount {

    private final BigDecimal percent;
    private final BigDecimal minimumSubtotal;

    public PercentageDiscount(String code, BigDecimal percent, BigDecimal minimumSubtotal) {
        super(code);
        this.percent = percent;
        this.minimumSubtotal = minimumSubtotal;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public BigDecimal getMinimumSubtotal() {
        return minimumSubtotal;
    }
}
