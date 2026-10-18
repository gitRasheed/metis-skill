package com.acme.ingest.metrics;

import com.acme.ingest.model.Event;
import com.acme.ingest.model.EventType;
import com.acme.ingest.model.IngestMetrics;
import com.acme.ingest.source.EventSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reads the batches itself so ops dashboards do not depend on the ingest job having run. */
public class MetricsServiceImpl implements MetricsService {

    private final EventSource eventSource;

    public MetricsServiceImpl(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    @Override
    public IngestMetrics compute(LocalDate day) {
        Map<EventType, Long> byType = new EnumMap<>(EventType.class);
        for (EventType type : EventType.values()) {
            byType.put(type, 0L);
        }
        Set<String> users = new HashSet<>();
        long processed = 0;
        for (Event event : eventSource.readAll()) {
            if (!LocalDate.ofInstant(event.timestamp(), ZoneOffset.UTC).equals(day)) {
                continue;
            }
            processed++;
            byType.merge(event.type(), 1L, Long::sum);
            users.add(event.userId());
        }
        return new IngestMetrics(day, processed, Map.copyOf(byType), users.size());
    }
}
