package com.acme.pricing.discount;

public abstract class Discount {

    private final String code;

    protected Discount(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
