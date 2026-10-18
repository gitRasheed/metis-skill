package com.acme.pricing.repository;

import com.acme.pricing.model.Customer;

import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findById(String id);

    void save(Customer customer);
}
