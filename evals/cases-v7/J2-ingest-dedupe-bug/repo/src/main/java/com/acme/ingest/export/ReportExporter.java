package com.acme.ingest.export;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public interface ReportExporter {

    /** Writes the day's summary and metrics files into outDir and returns their paths. */
    List<Path> export(LocalDate day, Path outDir);
}
