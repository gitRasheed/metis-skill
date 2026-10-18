package com.acme.ingest.job;

import com.acme.ingest.aggregate.EventAggregator;
import com.acme.ingest.model.DailySummary;
import com.acme.ingest.model.Event;
import com.acme.ingest.repository.SummaryRepository;
import com.acme.ingest.source.EventSource;

import java.time.LocalDate;
import java.util.List;

public class IngestJobImpl implements IngestJob {

    private final EventSource eventSource;
    private final EventAggregator aggregator;
    private final SummaryRepository summaryRepository;

    public IngestJobImpl(EventSource eventSource, EventAggregator aggregator, SummaryRepository summaryRepository) {
        this.eventSource = eventSource;
        this.aggregator = aggregator;
        this.summaryRepository = summaryRepository;
    }

    @Override
    public DailySummary run(LocalDate day) {
        List<Event> events = eventSource.readAll();
        DailySummary summary = aggregator.aggregate(events, day);
        summaryRepository.save(summary);
        return summary;
    }
}
