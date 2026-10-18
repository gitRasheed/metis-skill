package com.acme.pricing.config;

import com.acme.pricing.repository.CustomerRepository;
import com.acme.pricing.repository.DiscountRepository;
import com.acme.pricing.repository.InMemoryCustomerRepository;
import com.acme.pricing.repository.InMemoryDiscountRepository;
import com.acme.pricing.service.PricingService;
import com.acme.pricing.service.PricingServiceImpl;

/** Wires the pricing module. Services are singletons per context. */
public class AppContext {

    private final CustomerRepository customerRepository;
    private final DiscountRepository discountRepository;
    private final PricingService pricingService;

    public AppContext() {
        this.customerRepository = new InMemoryCustomerRepository();
        this.discountRepository = new InMemoryDiscountRepository();
        this.pricingService = new PricingServiceImpl(customerRepository, discountRepository);
    }

    public CustomerRepository customerRepository() {
        return customerRepository;
    }

    public DiscountRepository discountRepository() {
        return discountRepository;
    }

    public PricingService pricingService() {
        return pricingService;
    }
}
