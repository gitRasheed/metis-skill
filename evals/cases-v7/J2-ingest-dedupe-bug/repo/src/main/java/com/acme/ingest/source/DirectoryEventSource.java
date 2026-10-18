package com.acme.ingest.source;

import com.acme.ingest.model.Event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Reads every batch file in the data directory. Files with an unknown extension are skipped. */
public class DirectoryEventSource implements EventSource {

    private final Path dataDir;
    private final List<EventParser> parsers;

    public DirectoryEventSource(Path dataDir, List<EventParser> parsers) {
        this.dataDir = dataDir;
        this.parsers = List.copyOf(parsers);
    }

    @Override
    public List<Event> readAll() {
        List<Event> events = new ArrayList<>();
        for (Path file : batchFiles()) {
            for (EventParser parser : parsers) {
                if (parser.supports(file)) {
                    events.addAll(parser.parse(file));
                    break;
                }
            }
        }
        return events;
    }

    private List<Path> batchFiles() {
        try (Stream<Path> files = Files.list(dataDir)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
