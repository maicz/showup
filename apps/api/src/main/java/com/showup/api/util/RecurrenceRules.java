package com.showup.api.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Expands the subset of RFC 5545 RRULE that {@code EventSeries.recurrenceRule} actually needs:
 * {@code FREQ} (DAILY, WEEKLY, MONTHLY, YEARLY), {@code INTERVAL}, {@code COUNT}, {@code UNTIL}.
 *
 * <p>Anything richer — {@code BYDAY=TU,TH}, {@code BYSETPOS}, {@code EXDATE} — is rejected with a
 * named error rather than silently ignored, because quietly dropping a {@code BYDAY} produces a
 * schedule that looks plausible and is wrong.
 *
 * <p>Arithmetic runs in the series' zone, not in UTC: a weekly 19:00 meetup stays at 19:00 across
 * a daylight-saving boundary, which it would not if the recurrence just added 168 hours.
 */
public final class RecurrenceRules {

    private static final DateTimeFormatter UNTIL_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);

    private static final List<String> SUPPORTED = List.of("FREQ", "INTERVAL", "COUNT", "UNTIL");

    private RecurrenceRules() {
    }

    public enum Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }

    public record Rule(Frequency frequency, int interval, Integer count, Instant until) {
    }

    public static Rule parse(String rrule) {
        if (rrule == null || rrule.isBlank()) {
            throw new IllegalArgumentException("recurrence rule is empty");
        }
        Frequency frequency = null;
        int interval = 1;
        Integer count = null;
        Instant until = null;

        for (String part : rrule.trim().toUpperCase(Locale.ROOT).split(";")) {
            if (part.isBlank()) {
                continue;
            }
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("malformed recurrence part: " + part);
            }
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (!SUPPORTED.contains(key)) {
                throw new IllegalArgumentException(
                        key + " is not supported; this build understands " + String.join(", ", SUPPORTED));
            }
            switch (key) {
                case "FREQ" -> frequency = frequency(value);
                case "INTERVAL" -> interval = positiveInt(value, "INTERVAL");
                case "COUNT" -> count = positiveInt(value, "COUNT");
                case "UNTIL" -> until = until(value);
                default -> throw new IllegalStateException("unreachable");
            }
        }
        if (frequency == null) {
            throw new IllegalArgumentException("recurrence rule must specify FREQ");
        }
        return new Rule(frequency, interval, count, until);
    }

    /**
     * @param first the first occurrence; it is included in the result
     * @param limit a hard ceiling regardless of COUNT, so an unbounded rule cannot materialize
     *              an unbounded number of rows
     */
    public static List<Instant> expand(Rule rule, Instant first, ZoneId zone, Instant seriesUntil, int limit) {
        Instant ceiling = earliest(rule.until(), seriesUntil);
        int max = rule.count() == null ? limit : Math.min(rule.count(), limit);

        List<Instant> occurrences = new ArrayList<>();
        ZonedDateTime cursor = first.atZone(zone);
        for (int i = 0; i < max; i++) {
            Instant occurrence = cursor.toInstant();
            if (ceiling != null && occurrence.isAfter(ceiling)) {
                break;
            }
            occurrences.add(occurrence);
            cursor = switch (rule.frequency()) {
                case DAILY -> cursor.plusDays(rule.interval());
                case WEEKLY -> cursor.plusWeeks(rule.interval());
                case MONTHLY -> cursor.plusMonths(rule.interval());
                case YEARLY -> cursor.plusYears(rule.interval());
            };
        }
        return occurrences;
    }

    private static Frequency frequency(String value) {
        try {
            return Frequency.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsupported FREQ=" + value
                    + "; expected one of DAILY, WEEKLY, MONTHLY, YEARLY");
        }
    }

    private static int positiveInt(String value, String key) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException(key + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number, got " + value);
        }
    }

    private static Instant until(String value) {
        try {
            return ZonedDateTime.parse(value, UNTIL_FORMAT.withZone(ZoneId.of("UTC"))).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("UNTIL must look like 20261231T235959Z, got " + value);
        }
    }

    private static Instant earliest(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }
}
