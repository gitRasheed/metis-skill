package com.acme.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MoneyRounding {

    private MoneyRounding() {
    }

    // Reports reconcile stored quotes to the cent; changing this breaks historical comparisons.
    static BigDecimal toCents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
