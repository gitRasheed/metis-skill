package com.acme.pricing.repository;

import com.acme.pricing.discount.Discount;

import java.util.List;

public interface DiscountRepository {

    List<Discount> findAll();

    void save(Discount discount);

    void deleteByCode(String code);
}
