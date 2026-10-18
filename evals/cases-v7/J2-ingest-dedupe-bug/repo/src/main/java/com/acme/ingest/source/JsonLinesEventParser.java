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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One flat JSON object per line; producers never send nested values or escaped quotes. */
public class JsonLinesEventParser implements EventParser {

    private static final Pattern OBJECT = Pattern.compile("^\\{(.*)}$");
    private static final Pattern FIELD = Pattern.compile("\\s*\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|[^,\\s]+)\\s*(,|$)");

    @Override
    public boolean supports(Path file) {
        return file.getFileName().toString().endsWith(".jsonl");
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
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                events.add(EventFields.toEvent(parseObject(line)));
            } catch (RuntimeException e) {
                throw new MalformedEventException(file, i + 1, e.getMessage(), e);
            }
        }
        return events;
    }

    static Map<String, String> parseObject(String line) {
        Matcher object = OBJECT.matcher(line);
        if (!object.matches()) {
            throw new IllegalArgumentException("not a JSON object");
        }
        String body = object.group(1);
        Map<String, String> fields = new HashMap<>();
        Matcher field = FIELD.matcher(body);
        int end = 0;
        while (end < body.length() && field.find(end) && field.start() == end) {
            String value = field.group(3) != null ? field.group(3) : field.group(2);
            fields.put(field.group(1), value.equals("null") ? null : value);
            end = field.end();
        }
        if (end < body.trim().length()) {
            throw new IllegalArgumentException("unparseable JSON near position " + end);
        }
        return fields;
    }
}
