package com.acme.ingest.source;

import java.nio.file.Path;

public class MalformedEventException extends RuntimeException {

    public MalformedEventException(Path file, int lineNumber, String reason, Throwable cause) {
        super(file.getFileName() + ":" + lineNumber + ": " + reason, cause);
    }
}
