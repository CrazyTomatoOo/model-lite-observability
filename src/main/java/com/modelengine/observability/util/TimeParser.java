package com.modelengine.observability.util;

import com.modelengine.observability.exception.ObservabilityException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses time values from either RFC 3339 strings or Unix epoch second numbers.
 */
public final class TimeParser {

    private TimeParser() {
        // utility class
    }

    /**
     * Parses an Object to {@link Instant}.
     * <ul>
     *   <li>{@link String} → parsed as RFC 3339 (with Z or offset) via {@link Instant#parse(CharSequence)}
     *       or as local datetime (without timezone) interpreted as Asia/Shanghai (Beijing Time)</li>
     *   <li>{@link Number} → treated as Unix seconds, {@link Instant#ofEpochSecond(long)}</li>
     *   <li>Any other type or parse failure → {@link ObservabilityException} with BAD_REQUEST</li>
     * </ul>
     *
     * @param raw the raw value (String or Number)
     * @return the parsed Instant
     * @throws ObservabilityException if the value cannot be parsed
     */
    public static Instant parseTime(Object raw) {
        if (raw == null) {
            throw ObservabilityException.badRequest("startTime and endTime are required");
        }

        if (raw instanceof String s) {
            try {
                // Try RFC 3339 with explicit timezone first (e.g., 2025-01-01T00:00:00Z)
                return Instant.parse(s);
            } catch (DateTimeParseException e) {
                try {
                    // Try local datetime without timezone, interpret as Asia/Shanghai (Beijing Time)
                    LocalDateTime localDateTime = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
                } catch (DateTimeParseException e2) {
                    throw ObservabilityException.badRequest(
                            "Invalid startTime/endTime format: '" + s + "'. Expected RFC 3339 (e.g. 2025-01-01T00:00:00Z) or local datetime (e.g. 2025-01-01T00:00:00)");
                }
            }
        }

        if (raw instanceof Number n) {
            long epochSeconds = n.longValue();
            if (epochSeconds < 0) {
                throw ObservabilityException.badRequest(
                        "Invalid startTime/endTime: Unix timestamp must be non-negative, got " + epochSeconds);
            }
            return Instant.ofEpochSecond(epochSeconds);
        }

        throw ObservabilityException.badRequest(
                "startTime/endTime must be a string (RFC 3339) or a number (Unix timestamp in seconds), got "
                        + raw.getClass().getSimpleName());
    }
}
