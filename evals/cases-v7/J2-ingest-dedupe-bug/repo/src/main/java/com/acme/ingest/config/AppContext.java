package com.acme.ingest.config;

import com.acme.ingest.aggregate.EventAggregator;
import com.acme.ingest.aggregate.EventAggregatorImpl;
import com.acme.ingest.export.CsvReportExporter;
import com.acme.ingest.export.ReportExporter;
import com.acme.ingest.job.IngestJob;
import com.acme.ingest.job.IngestJobImpl;
import com.acme.ingest.metrics.MetricsService;
import com.acme.ingest.metrics.MetricsServiceImpl;
import com.acme.ingest.repository.InMemorySummaryRepository;
import com.acme.ingest.repository.SummaryRepository;
import com.acme.ingest.source.CsvEventParser;
import com.acme.ingest.source.DirectoryEventSource;
import com.acme.ingest.source.EventSource;
import com.acme.ingest.source.JsonLinesEventParser;

import java.nio.file.Path;
import java.util.List;

public class AppContext {

    private final EventSource eventSource;
    private final SummaryRepository summaryRepository;
    private final IngestJob ingestJob;
    private final MetricsService metricsService;
    private final ReportExporter reportExporter;

    public AppContext(Path dataDir) {
        this.eventSource = new DirectoryEventSource(dataDir, List.of(new JsonLinesEventParser(), new CsvEventParser()));
        this.summaryRepository = new InMemorySummaryRepository();
        EventAggregator aggregator = new EventAggregatorImpl();
        this.ingestJob = new IngestJobImpl(eventSource, aggregator, summaryRepository);
        this.metricsService = new MetricsServiceImpl(eventSource);
        this.reportExporter = new CsvReportExporter(summaryRepository, metricsService);
    }

    public EventSource eventSource() {
        return eventSource;
    }

    public SummaryRepository summaryRepository() {
        return summaryRepository;
    }

    public IngestJob ingestJob() {
        return ingestJob;
    }

    public MetricsService metricsService() {
        return metricsService;
    }

    public ReportExporter reportExporter() {
        return reportExporter;
    }
}
