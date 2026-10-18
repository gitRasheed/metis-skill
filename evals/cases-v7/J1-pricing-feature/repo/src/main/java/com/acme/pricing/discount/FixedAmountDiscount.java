package com.acme.pricing.discount;

import java.math.BigDecimal;

public class FixedAmountDiscount extends Discount {

    private final BigDecimal amount;
    private final BigDecimal minimumSubtotal;

    public FixedAmountDiscount(String code, BigDecimal amount, BigDecimal minimumSubtotal) {
        super(code);
        this.amount = amount;
        this.minimumSubtotal = minimumSubtotal;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getMinimumSubtotal() {
        return minimumSubtotal;
    }
}
