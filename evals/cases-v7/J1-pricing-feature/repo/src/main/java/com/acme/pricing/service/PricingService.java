package com.acme.pricing.service;

import com.acme.pricing.model.Quote;
import com.acme.pricing.model.QuoteRequest;

public interface PricingService {

    Quote quote(QuoteRequest request);
}
