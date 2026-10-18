package com.acme.ingest.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailySummary(LocalDate day, List<UserTotal> users) {

    public BigDecimal netTotal() {
        return users.stream().map(UserTotal::netAmount).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}
