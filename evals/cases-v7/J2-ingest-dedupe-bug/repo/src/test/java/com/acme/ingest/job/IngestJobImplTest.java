package com.acme.ingest.job;

import com.acme.ingest.config.AppContext;
import com.acme.ingest.model.DailySummary;
import com.acme.ingest.model.UserTotal;
import com.acme.ingest.testing.TempData;
import com.acme.ingest.testing.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.acme.ingest.testing.Assert.assertEquals;

public class IngestJobImplTest {

    private static final LocalDate DAY = LocalDate.parse("2026-10-17");

    @Test
    public void combinesBatchFilesIntoPerUserTotals() {
        TempData data = new TempData()
                .file("batch-001.jsonl",
                        "{\"eventId\":\"evt-1001\",\"userId\":\"u1\",\"type\":\"PAYMENT\",\"amount\":40.00,\"timestamp\":\"2026-10-17T08:00:00Z\"}",
                        "{\"eventId\":\"evt-1002\",\"userId\":\"u2\",\"type\":\"PAYMENT\",\"amount\":15.50,\"timestamp\":\"2026-10-17T08:05:00Z\"}",
                        "{\"eventId\":\"evt-1003\",\"userId\":\"u1\",\"type\":\"PAYMENT\",\"amount\":25.00,\"timestamp\":\"2026-10-17T08:10:00Z\"}")
                .file("batch-002.csv",
                        "event_id,user_id,type,amount,timestamp",
                        "evt-1003,u1,PAYMENT,25.00,2026-10-17T08:10:00Z",
                        "evt-1004,u2,REFUND,5.50,2026-10-17T09:00:00Z",
                        "evt-1005,u1,VIEW,0,2026-10-17T09:30:00Z");

        DailySummary summary = new AppContext(data.dir()).ingestJob().run(DAY);

        assertEquals(List.of(
                new UserTotal("u1", new BigDecimal("90.00"), 4),
                new UserTotal("u2", new BigDecimal("10.00"), 2)), summary.users());
        assertEquals(new BigDecimal("100.00"), summary.netTotal());
    }

    @Test
    public void onlyEventsFromTheRequestedUtcDayCount() {
        TempData data = new TempData().file("batch.csv",
                "event_id,user_id,type,amount,timestamp",
                "e-1,u1,PAYMENT,10.00,2026-10-16T23:59:59Z",
                "e-2,u1,PAYMENT,20.00,2026-10-17T00:00:00Z",
                "e-3,u1,PAYMENT,30.00,2026-10-18T00:00:00Z");

        DailySummary summary = new AppContext(data.dir()).ingestJob().run(DAY);

        assertEquals(List.of(new UserTotal("u1", new BigDecimal("20.00"), 1)), summary.users());
    }

    @Test
    public void storesSummaryAndRerunReplacesIt() {
        TempData data = new TempData().file("batch.csv",
                "event_id,user_id,type,amount,timestamp",
                "e-1,u1,PAYMENT,10.00,2026-10-17T10:00:00Z");
        AppContext context = new AppContext(data.dir());

        context.ingestJob().run(DAY);
        data.file("batch-2.csv",
                "event_id,user_id,type,amount,timestamp",
                "e-2,u1,PAYMENT,5.00,2026-10-17T11:00:00Z");
        context.ingestJob().run(DAY);

        assertEquals(new BigDecimal("15.00"), context.summaryRepository().findByDay(DAY).orElseThrow().netTotal());
    }

    @Test
    public void emptyDataDirectoryGivesEmptySummary() {
        DailySummary summary = new AppContext(new TempData().dir()).ingestJob().run(DAY);

        assertEquals(List.of(), summary.users());
        assertEquals(new BigDecimal("0.00"), summary.netTotal());
    }
}
