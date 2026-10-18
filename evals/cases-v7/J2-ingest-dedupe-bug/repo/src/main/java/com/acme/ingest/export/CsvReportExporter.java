package com.acme.ingest.export;

import com.acme.ingest.metrics.MetricsService;
import com.acme.ingest.model.DailySummary;
import com.acme.ingest.model.EventType;
import com.acme.ingest.model.IngestMetrics;
import com.acme.ingest.model.UserTotal;
import com.acme.ingest.repository.SummaryRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CsvReportExporter implements ReportExporter {

    private final SummaryRepository summaryRepository;
    private final MetricsService metricsService;

    public CsvReportExporter(SummaryRepository summaryRepository, MetricsService metricsService) {
        this.summaryRepository = summaryRepository;
        this.metricsService = metricsService;
    }

    @Override
    public List<Path> export(LocalDate day, Path outDir) {
        DailySummary summary = summaryRepository.findByDay(day)
                .orElseThrow(() -> new IllegalStateException("No summary stored for " + day + "; run the ingest job first"));
        IngestMetrics metrics = metricsService.compute(day);

        List<String> summaryLines = new ArrayList<>();
        summaryLines.add("user_id,net_amount,event_count");
        for (UserTotal user : summary.users()) {
            summaryLines.add(user.userId() + "," + user.netAmount().toPlainString() + "," + user.eventCount());
        }

        List<String> metricLines = new ArrayList<>();
        metricLines.add("metric,value");
        metricLines.add("events_processed," + metrics.eventsProcessed());
        for (EventType type : EventType.values()) {
            metricLines.add("events_" + type.name().toLowerCase(Locale.ROOT) + "," + metrics.eventsByType().get(type));
        }
        metricLines.add("distinct_users," + metrics.distinctUsers());

        try {
            Files.createDirectories(outDir);
            Path summaryFile = Files.write(outDir.resolve("summary-" + day + ".csv"), summaryLines);
            Path metricsFile = Files.write(outDir.resolve("metrics-" + day + ".csv"), metricLines);
            return List.of(summaryFile, metricsFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
