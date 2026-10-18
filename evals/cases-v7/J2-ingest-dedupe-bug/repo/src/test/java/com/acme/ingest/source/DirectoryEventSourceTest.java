package com.acme.ingest.source;

import com.acme.ingest.model.Event;
import com.acme.ingest.model.EventType;
import com.acme.ingest.testing.TempData;
import com.acme.ingest.testing.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.acme.ingest.testing.Assert.assertEquals;
import static com.acme.ingest.testing.Assert.assertThrows;
import static com.acme.ingest.testing.Assert.assertTrue;

public class DirectoryEventSourceTest {

    private static EventSource sourceFor(TempData data) {
        return new DirectoryEventSource(data.dir(), List.of(new JsonLinesEventParser(), new CsvEventParser()));
    }

    @Test
    public void readsJsonLinesAndCsvInFileNameOrder() {
        TempData data = new TempData()
                .file("b-002.csv",
                        "event_id,user_id,type,amount,timestamp",
                        "e-2,u-9,REFUND,3.10,2026-10-17T11:00:00Z")
                .file("b-001.jsonl",
                        "{\"eventId\":\"e-1\",\"userId\":\"u-9\",\"type\":\"PAYMENT\",\"amount\":12.50,\"timestamp\":\"2026-10-17T10:00:00Z\"}");

        List<Event> events = sourceFor(data).readAll();

        assertEquals(List.of(
                new Event("e-1", "u-9", EventType.PAYMENT, new BigDecimal("12.50"), Instant.parse("2026-10-17T10:00:00Z")),
                new Event("e-2", "u-9", EventType.REFUND, new BigDecimal("3.10"), Instant.parse("2026-10-17T11:00:00Z"))),
                events);
    }

    @Test
    public void csvColumnsMayBeReordered() {
        TempData data = new TempData().file("b.csv",
                "timestamp,amount,type,user_id,event_id",
                "2026-10-17T10:00:00Z,7.00,PAYMENT,u-1,e-7");

        assertEquals("e-7", sourceFor(data).readAll().get(0).eventId());
    }

    @Test
    public void skipsBlankLinesAndUnknownFiles() {
        TempData data = new TempData()
                .file("notes.txt", "not a batch")
                .file("b.jsonl", "",
                        "{\"eventId\":\"e-1\",\"userId\":\"u-1\",\"type\":\"VIEW\",\"amount\":0,\"timestamp\":\"2026-10-17T10:00:00Z\"}",
                        "   ");

        assertEquals(1, sourceFor(data).readAll().size());
    }

    @Test
    public void malformedLineReportsFileAndLine() {
        TempData data = new TempData().file("b.jsonl",
                "{\"eventId\":\"e-1\",\"userId\":\"u-1\",\"type\":\"VIEW\",\"amount\":0,\"timestamp\":\"2026-10-17T10:00:00Z\"}",
                "{\"eventId\":\"e-2\",\"userId\":\"u-1\",\"type\":\"PAYMENT\"}");

        MalformedEventException error = assertThrows(MalformedEventException.class, () -> sourceFor(data).readAll());
        assertTrue(error.getMessage().startsWith("b.jsonl:2:"), error.getMessage());
    }
}
