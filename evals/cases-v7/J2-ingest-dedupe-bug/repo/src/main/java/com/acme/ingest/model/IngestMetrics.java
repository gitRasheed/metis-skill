package com.acme.ingest.model;

import java.time.LocalDate;
import java.util.Map;

public record IngestMetrics(LocalDate day, long eventsProcessed, Map<EventType, Long> eventsByType, long distinctUsers) {
}
