package com.acme.ingest.aggregate;

import com.acme.ingest.model.DailySummary;
import com.acme.ingest.model.Event;
import com.acme.ingest.model.UserTotal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EventAggregatorImpl implements EventAggregator {

    @Override
    public DailySummary aggregate(List<Event> events, LocalDate day) {
        Map<String, BigDecimal> netByUser = new TreeMap<>();
        Map<String, Long> countByUser = new TreeMap<>();
        for (Event event : events) {
            if (!LocalDate.ofInstant(event.timestamp(), ZoneOffset.UTC).equals(day)) {
                continue;
            }
            BigDecimal signed = switch (event.type()) {
                case PAYMENT -> event.amount();
                case REFUND -> event.amount().negate();
                case VIEW -> BigDecimal.ZERO;
            };
            netByUser.merge(event.userId(), signed, BigDecimal::add);
            countByUser.merge(event.userId(), 1L, Long::sum);
        }
        List<UserTotal> users = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : netByUser.entrySet()) {
            users.add(new UserTotal(
                    entry.getKey(),
                    entry.getValue().setScale(2, RoundingMode.HALF_EVEN),
                    countByUser.get(entry.getKey())));
        }
        return new DailySummary(day, List.copyOf(users));
    }
}
