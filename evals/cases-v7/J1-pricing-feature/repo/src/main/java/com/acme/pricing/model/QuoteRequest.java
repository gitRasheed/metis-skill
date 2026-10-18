package com.acme.pricing.model;

import java.util.List;

public record QuoteRequest(String customerId, List<LineItem> items) {
}
