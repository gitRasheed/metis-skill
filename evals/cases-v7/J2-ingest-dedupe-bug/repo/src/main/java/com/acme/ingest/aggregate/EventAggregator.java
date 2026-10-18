package com.acme.ingest.aggregate;

import com.acme.ingest.model.DailySummary;
import com.acme.ingest.model.Event;

import java.time.LocalDate;
import java.util.List;

public interface EventAggregator {

    DailySummary aggregate(List<Event> events, LocalDate day);
}
