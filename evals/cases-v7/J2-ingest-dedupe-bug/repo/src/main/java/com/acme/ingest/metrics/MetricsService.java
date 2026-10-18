package com.acme.ingest.metrics;

import com.acme.ingest.model.IngestMetrics;

import java.time.LocalDate;

public interface MetricsService {

    IngestMetrics compute(LocalDate day);
}
