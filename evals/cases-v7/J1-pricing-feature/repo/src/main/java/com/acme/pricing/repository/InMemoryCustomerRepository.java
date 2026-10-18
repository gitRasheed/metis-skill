package com.acme.pricing.repository;

import com.acme.pricing.model.Customer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers = new ConcurrentHashMap<>();

    @Override
    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(customers.get(id));
    }

    @Override
    public void save(Customer customer) {
        customers.put(customer.id(), customer);
    }
}
