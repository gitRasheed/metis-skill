package com.acme.pricing.service;

public class UnknownCustomerException extends RuntimeException {

    public UnknownCustomerException(String customerId) {
        super("Unknown customer: " + customerId);
    }
}
