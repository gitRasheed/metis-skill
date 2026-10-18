package hiddencheck;

import com.acme.ingest.config.AppContext;
import com.acme.ingest.model.DailySummary;
import com.acme.ingest.model.EventType;
import com.acme.ingest.model.IngestMetrics;
import com.acme.ingest.model.UserTotal;
import com.acme.ingest.source.MalformedEventException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static hiddencheck.HiddenRunner.eq;

public class IngestHiddenChecks {

    private static final LocalDate DAY = LocalDate.parse("2026-10-17");
    private static final String CSV_HEADER = "event_id,user_id,type,amount,timestamp";

    private final Path dir;

    public IngestHiddenChecks() {
        try {
            dir = Files.createTempDirectory("hidden-ingest");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private IngestHiddenChecks file(String name, String... lines) {
        try {
            Files.write(dir.resolve(name), List.of(lines));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    private static String json(String id, String user, String type, String amount, String ts) {
        return "{\"eventId\":\"" + id + "\",\"userId\":\"" + user + "\",\"type\":\"" + type
                + "\",\"amount\":" + amount + ",\"timestamp\":\"" + ts + "\"}";
    }

    private static UserTotal user(String id, String net, long count) {
        return new UserTotal(id, new BigDecimal(net), count);
    }

    private static Map<EventType, Long> byType(long payment, long refund, long view) {
        return Map.of(EventType.PAYMENT, payment, EventType.REFUND, refund, EventType.VIEW, view);
    }

    private static void metricsEq(IngestMetrics metrics, long processed, Map<EventType, Long> types, long users) {
        eq(processed, metrics.eventsProcessed(), "events processed");
        eq(types, metrics.eventsByType(), "events by type");
        eq(users, metrics.distinctUsers(), "distinct users");
    }

    @Check
    public void duplicateWithinOneFileCountedOnceInBothPaths() {
        file("b-1.jsonl",
                json("e-1", "u1", "PAYMENT", "30.00", "2026-10-17T10:00:00Z"),
                json("e-2", "u1", "PAYMENT", "5.00", "2026-10-17T10:05:00Z"),
                json("e-1", "u1", "PAYMENT", "30.00", "2026-10-17T10:00:00Z"));
        AppContext ctx = new AppContext(dir);

        eq(List.of(user("u1", "35.00", 2)), ctx.ingestJob().run(DAY).users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), 2, byType(2, 0, 0), 1);
    }

    @Check
    public void duplicateAcrossFilesAndFormatsCountedOnce() {
        file("b-1.jsonl",
                json("e-1", "u1", "PAYMENT", "30.00", "2026-10-17T10:00:00Z"),
                json("e-2", "u2", "REFUND", "4.00", "2026-10-17T10:05:00Z"));
        file("b-2.csv", CSV_HEADER,
                "e-2,u2,REFUND,4.00,2026-10-17T10:05:00Z",
                "e-1,u1,PAYMENT,30.00,2026-10-17T10:00:00Z",
                "e-3,u2,PAYMENT,10.00,2026-10-17T11:00:00Z");
        file("b-3.jsonl", json("e-1", "u1", "PAYMENT", "30.00", "2026-10-17T10:00:00Z"));
        AppContext ctx = new AppContext(dir);

        eq(List.of(user("u1", "30.00", 1), user("u2", "6.00", 2)), ctx.ingestJob().run(DAY).users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), 3, byType(2, 1, 0), 2);
    }

    @Check
    public void metricsDedupedWithoutRunningIngestFirst() {
        file("b-1.csv", CSV_HEADER,
                "e-1,u1,VIEW,0,2026-10-17T10:00:00Z",
                "e-1,u1,VIEW,0,2026-10-17T10:00:00Z",
                "e-2,u2,PAYMENT,1.00,2026-10-17T10:00:00Z");
        file("b-2.csv", CSV_HEADER, "e-2,u2,PAYMENT,1.00,2026-10-17T10:00:00Z");

        metricsEq(new AppContext(dir).metricsService().compute(DAY), 2, byType(1, 0, 1), 2);
    }

    @Check
    public void distinctIdsWithIdenticalPayloadsAreBothCounted() {
        file("b-1.jsonl",
                json("e-1", "u1", "PAYMENT", "12.00", "2026-10-17T10:00:00Z"),
                json("e-2", "u1", "PAYMENT", "12.00", "2026-10-17T10:00:00Z"));
        file("b-2.csv", CSV_HEADER, "e-3,u1,PAYMENT,12.00,2026-10-17T10:00:00Z");
        AppContext ctx = new AppContext(dir);

        eq(List.of(user("u1", "36.00", 3)), ctx.ingestJob().run(DAY).users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), 3, byType(3, 0, 0), 1);
    }

    @Check
    public void correctedExpectationForCombinedBatchFixture() {
        file("batch-001.jsonl",
                json("evt-1001", "u1", "PAYMENT", "40.00", "2026-10-17T08:00:00Z"),
                json("evt-1002", "u2", "PAYMENT", "15.50", "2026-10-17T08:05:00Z"),
                json("evt-1003", "u1", "PAYMENT", "25.00", "2026-10-17T08:10:00Z"));
        file("batch-002.csv", CSV_HEADER,
                "evt-1003,u1,PAYMENT,25.00,2026-10-17T08:10:00Z",
                "evt-1004,u2,REFUND,5.50,2026-10-17T09:00:00Z",
                "evt-1005,u1,VIEW,0,2026-10-17T09:30:00Z");
        AppContext ctx = new AppContext(dir);

        DailySummary summary = ctx.ingestJob().run(DAY);
        eq(List.of(user("u1", "65.00", 3), user("u2", "10.00", 2)), summary.users(), "summary");
        eq(new BigDecimal("75.00"), summary.netTotal(), "net total");
        metricsEq(ctx.metricsService().compute(DAY), 5, byType(3, 1, 1), 2);
    }

    @Check
    public void sampleDayExportMatchesProvider() throws IOException {
        file("events-2026-10-17-0001.jsonl",
                json("7f3a-0001", "user-104", "PAYMENT", "49.99", "2026-10-17T06:12:44Z"),
                json("7f3a-0002", "user-221", "VIEW", "0", "2026-10-17T06:13:02Z"),
                json("7f3a-0003", "user-221", "PAYMENT", "12.00", "2026-10-17T06:15:10Z"),
                json("7f3a-0004", "user-390", "PAYMENT", "120.00", "2026-10-17T07:01:33Z"),
                json("7f3a-0005", "user-104", "VIEW", "0", "2026-10-17T07:20:00Z"),
                json("7f3a-0006", "user-390", "REFUND", "20.00", "2026-10-17T08:45:12Z"));
        file("events-2026-10-17-0002.jsonl",
                json("7f3a-0004", "user-390", "PAYMENT", "120.00", "2026-10-17T07:01:33Z"),
                json("7f3a-0005", "user-104", "VIEW", "0", "2026-10-17T07:20:00Z"),
                json("7f3a-0007", "user-104", "PAYMENT", "15.25", "2026-10-17T11:02:09Z"),
                json("7f3a-0008", "user-512", "PAYMENT", "8.75", "2026-10-17T12:30:00Z"),
                json("7f3a-0009", "user-221", "PAYMENT", "12.00", "2026-10-17T13:14:51Z"));
        file("events-2026-10-17-0003.csv", CSV_HEADER,
                "7f3a-0009,user-221,PAYMENT,12.00,2026-10-17T13:14:51Z",
                "7f3a-0010,user-512,VIEW,0,2026-10-17T15:40:00Z",
                "7f3a-0011,user-390,PAYMENT,64.10,2026-10-17T18:22:37Z",
                "7f3a-0012,user-104,PAYMENT,5.00,2026-10-17T23:59:59Z",
                "7f3a-0013,user-104,PAYMENT,30.00,2026-10-18T00:00:01Z");
        AppContext ctx = new AppContext(dir);
        Path out = Files.createTempDirectory("hidden-out").resolve("reports");

        ctx.ingestJob().run(DAY);
        List<Path> files = ctx.reportExporter().export(DAY, out);

        eq(List.of(out.resolve("summary-2026-10-17.csv"), out.resolve("metrics-2026-10-17.csv")), files, "export paths");
        eq(List.of("user_id,net_amount,event_count",
                "user-104,70.24,4",
                "user-221,24.00,3",
                "user-390,164.10,3",
                "user-512,8.75,2"), Files.readAllLines(files.get(0)), "summary file");
        eq(List.of("metric,value",
                "events_processed,12",
                "events_payment,8",
                "events_refund,1",
                "events_view,3",
                "distinct_users,4"), Files.readAllLines(files.get(1)), "metrics file");
    }

    @Check
    public void repeatedRunsAndReadsStayCorrect() {
        file("b-1.csv", CSV_HEADER,
                "e-1,u1,PAYMENT,10.00,2026-10-17T10:00:00Z",
                "e-1,u1,PAYMENT,10.00,2026-10-17T10:00:00Z",
                "e-2,u1,PAYMENT,2.50,2026-10-17T11:00:00Z");
        AppContext ctx = new AppContext(dir);

        for (int run = 1; run <= 3; run++) {
            eq(List.of(user("u1", "12.50", 2)), ctx.ingestJob().run(DAY).users(), "summary on run " + run);
            metricsEq(ctx.metricsService().compute(DAY), 2, byType(2, 0, 0), 1);
        }
        eq(List.of(), ctx.ingestJob().run(LocalDate.parse("2026-10-18")).users(), "other day");
        eq(List.of(user("u1", "12.50", 2)), ctx.summaryRepository().findByDay(DAY).orElseThrow().users(), "stored");
    }

    @Check
    public void unrelatedBehaviourUnchanged() {
        file("a.csv", "timestamp,amount,type,user_id,event_id",
                "2026-10-16T23:59:59Z,99.00,PAYMENT,u3,x-0",
                "2026-10-17T00:00:00Z,20.00,PAYMENT,u2,x-1",
                "2026-10-17T12:00:00Z,7.25,REFUND,u2,x-2",
                "2026-10-17T13:00:00Z,0,VIEW,u1,x-3",
                "2026-10-18T00:00:00Z,50.00,PAYMENT,u1,x-4");
        file("notes.txt", "ignored");
        AppContext ctx = new AppContext(dir);

        DailySummary summary = ctx.ingestJob().run(DAY);
        eq(List.of(user("u1", "0.00", 1), user("u2", "12.75", 2)), summary.users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), 3, byType(1, 1, 1), 2);
    }

    @Check
    public void redeliveryWithDifferentAmountFormattingCountedOnce() {
        file("b-1.jsonl", json("e-1", "u1", "PAYMENT", "1.0", "2026-10-17T10:00:00Z"));
        file("b-2.csv", CSV_HEADER,
                "e-1,u1,PAYMENT,1.00,2026-10-17T10:00:00Z",
                "e-2,u1,PAYMENT,2.50,2026-10-17T10:30:00Z");
        AppContext ctx = new AppContext(dir);

        eq(List.of(user("u1", "3.50", 2)), ctx.ingestJob().run(DAY).users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), 2, byType(2, 0, 0), 1);
    }

    @Check
    public void eventIdsAreCaseSensitive() {
        file("b-1.csv", CSV_HEADER,
                "E-1,u1,PAYMENT,4.00,2026-10-17T10:00:00Z",
                "e-1,u1,PAYMENT,4.00,2026-10-17T10:00:00Z");
        AppContext ctx = new AppContext(dir);

        eq(List.of(user("u1", "8.00", 2)), ctx.ingestJob().run(DAY).users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), 2, byType(2, 0, 0), 1);
    }

    @Check
    public void dedupeHoldsAcrossLargeBatches() {
        int distinct = 3000;
        String[] first = new String[distinct + 1];
        first[0] = CSV_HEADER;
        for (int i = 0; i < distinct; i++) {
            first[i + 1] = String.format("evt-%05d,u%d,PAYMENT,1.00,2026-10-17T10:00:00Z", i, i % 5);
        }
        String[] redelivered = new String[101];
        redelivered[0] = CSV_HEADER;
        for (int i = 0; i < 50; i++) {
            redelivered[i + 1] = first[i + 1];
            redelivered[i + 51] = first[distinct - i];
        }
        file("b-1.csv", first);
        file("b-2.csv", redelivered);
        AppContext ctx = new AppContext(dir);

        eq(List.of(user("u0", "600.00", 600), user("u1", "600.00", 600), user("u2", "600.00", 600),
                user("u3", "600.00", 600), user("u4", "600.00", 600)), ctx.ingestJob().run(DAY).users(), "summary");
        metricsEq(ctx.metricsService().compute(DAY), distinct, byType(distinct, 0, 0), 5);
    }

    @Check
    public void malformedLinesStillRejectedWithLocation() {
        file("b.jsonl",
                json("e-1", "u1", "PAYMENT", "1.00", "2026-10-17T10:00:00Z"),
                "{\"eventId\":\"e-2\",\"userId\":\"u1\"}");
        try {
            new AppContext(dir).ingestJob().run(DAY);
        } catch (MalformedEventException expected) {
            if (!expected.getMessage().startsWith("b.jsonl:2:")) {
                throw new AssertionError("unexpected message: " + expected.getMessage());
            }
            return;
        }
        throw new AssertionError("expected MalformedEventException");
    }

    @Check
    public void exportStillRequiresIngestFirst() {
        try {
            new AppContext(dir).reportExporter().export(DAY, dir.resolve("out"));
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("expected IllegalStateException");
    }
}
