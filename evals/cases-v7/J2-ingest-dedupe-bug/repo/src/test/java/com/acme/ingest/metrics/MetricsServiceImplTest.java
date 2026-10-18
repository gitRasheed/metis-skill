package com.acme.ingest.metrics;

import com.acme.ingest.config.AppContext;
import com.acme.ingest.model.EventType;
import com.acme.ingest.model.IngestMetrics;
import com.acme.ingest.testing.TempData;
import com.acme.ingest.testing.Test;

import java.time.LocalDate;
import java.util.Map;

import static com.acme.ingest.testing.Assert.assertEquals;

public class MetricsServiceImplTest {

    @Test
    public void countsEventsByTypeForTheDay() {
        TempData data = new TempData().file("batch.csv",
                "event_id,user_id,type,amount,timestamp",
                "e-1,u1,PAYMENT,10.00,2026-10-17T10:00:00Z",
                "e-2,u2,PAYMENT,4.00,2026-10-17T10:01:00Z",
                "e-3,u2,VIEW,0,2026-10-17T10:02:00Z",
                "e-4,u3,REFUND,1.00,2026-10-16T10:00:00Z");

        IngestMetrics metrics = new AppContext(data.dir()).metricsService().compute(LocalDate.parse("2026-10-17"));

        assertEquals(3L, metrics.eventsProcessed());
        assertEquals(Map.of(EventType.PAYMENT, 2L, EventType.REFUND, 0L, EventType.VIEW, 1L), metrics.eventsByType());
        assertEquals(2L, metrics.distinctUsers());
    }
}
