package com.acme.ingest.export;

import com.acme.ingest.config.AppContext;
import com.acme.ingest.testing.TempData;
import com.acme.ingest.testing.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static com.acme.ingest.testing.Assert.assertEquals;
import static com.acme.ingest.testing.Assert.assertThrows;

public class CsvReportExporterTest {

    private static final LocalDate DAY = LocalDate.parse("2026-10-17");

    @Test
    public void writesSummaryAndMetricsFiles() throws IOException {
        TempData data = new TempData().file("batch.csv",
                "event_id,user_id,type,amount,timestamp",
                "e-1,u2,PAYMENT,10.00,2026-10-17T10:00:00Z",
                "e-2,u1,PAYMENT,4.25,2026-10-17T10:01:00Z",
                "e-3,u1,REFUND,1.00,2026-10-17T10:02:00Z");
        AppContext context = new AppContext(data.dir());
        Path out = new TempData().dir().resolve("out");

        context.ingestJob().run(DAY);
        List<Path> files = context.reportExporter().export(DAY, out);

        assertEquals(List.of(out.resolve("summary-2026-10-17.csv"), out.resolve("metrics-2026-10-17.csv")), files);
        assertEquals(List.of("user_id,net_amount,event_count", "u1,3.25,2", "u2,10.00,1"), Files.readAllLines(files.get(0)));
        assertEquals(List.of("metric,value", "events_processed,3", "events_payment,2", "events_refund,1",
                "events_view,0", "distinct_users,2"), Files.readAllLines(files.get(1)));
    }

    @Test
    public void exportRequiresIngestFirst() {
        AppContext context = new AppContext(new TempData().dir());

        assertThrows(IllegalStateException.class, () -> context.reportExporter().export(DAY, new TempData().dir()));
    }
}
