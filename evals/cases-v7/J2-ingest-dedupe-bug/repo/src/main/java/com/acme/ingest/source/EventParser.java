package com.acme.ingest.source;

import com.acme.ingest.model.Event;

import java.nio.file.Path;
import java.util.List;

public interface EventParser {

    boolean supports(Path file);

    List<Event> parse(Path file);
}
