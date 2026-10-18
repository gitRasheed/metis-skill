package com.acme.ingest;

import com.acme.ingest.config.AppContext;
import com.acme.ingest.model.DailySummary;

import java.nio.file.Path;
import java.time.LocalDate;

public final class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage: Main <dataDir> <yyyy-mm-dd> <outDir>");
            System.exit(2);
        }
        AppContext context = new AppContext(Path.of(args[0]));
        LocalDate day = LocalDate.parse(args[1]);
        DailySummary summary = context.ingestJob().run(day);
        System.out.println("Summarised " + summary.users().size() + " users for " + day + ", net " + summary.netTotal());
        for (Path file : context.reportExporter().export(day, Path.of(args[2]))) {
            System.out.println("Wrote " + file);
        }
    }
}
