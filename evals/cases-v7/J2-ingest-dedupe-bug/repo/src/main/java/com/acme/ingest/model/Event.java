package com.acme.ingest.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Event(String eventId, String userId, EventType type, BigDecimal amount, Instant timestamp) {
}
