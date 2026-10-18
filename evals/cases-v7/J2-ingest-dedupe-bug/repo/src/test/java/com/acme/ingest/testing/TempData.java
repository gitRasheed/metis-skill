package com.acme.ingest.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TempData {

    private final Path dir;

    public TempData() {
        try {
            dir = Files.createTempDirectory("ingest-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path dir() {
        return dir;
    }

    public TempData file(String name, String... lines) {
        try {
            Files.write(dir.resolve(name), List.of(lines));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }
}
