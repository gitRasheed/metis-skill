package com.acme.ingest.source;

import com.acme.ingest.model.Event;
import com.acme.ingest.model.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

final class EventFields {

    private EventFields() {
    }

    static Event toEvent(Map<String, String> fields) {
        return new Event(
                required(fields, "eventId"),
                required(fields, "userId"),
                EventType.valueOf(required(fields, "type")),
                new BigDecimal(required(fields, "amount")),
                Instant.parse(required(fields, "timestamp")));
    }

    private static String required(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing field " + name);
        }
        return value;
    }
}
