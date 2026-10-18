package com.acme.pricing.model;

import java.math.BigDecimal;

public record AppliedDiscount(String code, BigDecimal amount) {
}
