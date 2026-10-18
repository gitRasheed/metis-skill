package com.acme.ingest.repository;

import com.acme.ingest.model.DailySummary;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySummaryRepository implements SummaryRepository {

    private final Map<LocalDate, DailySummary> summaries = new ConcurrentHashMap<>();

    @Override
    public void save(DailySummary summary) {
        summaries.put(summary.day(), summary);
    }

    @Override
    public Optional<DailySummary> findByDay(LocalDate day) {
        return Optional.ofNullable(summaries.get(day));
    }
}
