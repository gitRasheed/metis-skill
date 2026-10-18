package com.acme.ingest.model;

import java.math.BigDecimal;

/** Net amount is payments minus refunds; the count includes every event type. */
public record UserTotal(String userId, BigDecimal netAmount, long eventCount) {
}
