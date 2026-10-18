package com.acme.ingest.source;

import com.acme.ingest.model.Event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Header row required; columns may come in any order. Values are never quoted upstream. */
public class CsvEventParser implements EventParser {

    private static final Map<String, String> COLUMN_TO_FIELD = Map.of(
            "event_id", "eventId",
            "user_id", "userId",
            "type", "type",
            "amount", "amount",
            "timestamp", "timestamp");

    @Override
    public boolean supports(Path file) {
        return file.getFileName().toString().endsWith(".csv");
    }

    @Override
    public List<Event> parse(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<Event> events = new ArrayList<>();
        if (lines.isEmpty()) {
            return events;
        }
        String[] header = lines.get(0).trim().split(",");
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] values = line.split(",", -1);
            try {
                if (values.length != header.length) {
                    throw new IllegalArgumentException("expected " + header.length + " columns, got " + values.length);
                }
                Map<String, String> fields = new HashMap<>();
                for (int c = 0; c < header.length; c++) {
                    fields.put(COLUMN_TO_FIELD.getOrDefault(header[c].trim(), header[c].trim()), values[c].trim());
                }
                events.add(EventFields.toEvent(fields));
            } catch (RuntimeException e) {
                throw new MalformedEventException(file, i + 1, e.getMessage(), e);
            }
        }
        return events;
    }
}
