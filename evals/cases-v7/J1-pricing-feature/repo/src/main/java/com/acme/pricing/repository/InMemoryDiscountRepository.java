package com.acme.pricing.repository;

import com.acme.pricing.discount.Discount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryDiscountRepository implements DiscountRepository {

    private final Map<String, Discount> discountsByCode = new LinkedHashMap<>();

    @Override
    public synchronized List<Discount> findAll() {
        return new ArrayList<>(discountsByCode.values());
    }

    @Override
    public synchronized void save(Discount discount) {
        discountsByCode.put(discount.getCode(), discount);
    }

    @Override
    public synchronized void deleteByCode(String code) {
        discountsByCode.remove(code);
    }
}
