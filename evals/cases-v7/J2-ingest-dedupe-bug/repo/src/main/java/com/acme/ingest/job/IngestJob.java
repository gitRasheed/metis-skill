package com.acme.ingest.job;

import com.acme.ingest.model.DailySummary;

import java.time.LocalDate;

public interface IngestJob {

    /** Builds and stores the per-user summary for one UTC day. Re-running replaces the stored summary. */
    DailySummary run(LocalDate day);
}
