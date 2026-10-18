package com.acme.pricing.model;

import java.math.BigDecimal;

public record LineItem(String sku, int quantity, BigDecimal unitPrice) {

    public BigDecimal lineAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
