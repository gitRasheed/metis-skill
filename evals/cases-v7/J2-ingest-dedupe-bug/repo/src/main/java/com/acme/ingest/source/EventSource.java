package com.acme.ingest.source;

import com.acme.ingest.model.Event;

import java.util.List;

public interface EventSource {

    /** Every event from every batch, in batch file name order. */
    List<Event> readAll();
}
