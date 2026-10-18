package com.acme.ingest.repository;

import com.acme.ingest.model.DailySummary;

import java.time.LocalDate;
import java.util.Optional;

public interface SummaryRepository {

    void save(DailySummary summary);

    Optional<DailySummary> findByDay(LocalDate day);
}
