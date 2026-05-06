package com.modelengine.observability.util;

import com.modelengine.observability.exception.ObservabilityException;

import java.time.Instant;

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
     *   <li>{@link String} → parsed as RFC 3339 via {@link Instant#parse(CharSequence)}</li>
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
                return Instant.parse(s);
            } catch (Exception e) {
                throw ObservabilityException.badRequest(
                        "Invalid startTime/endTime format: '" + s + "'. Expected RFC 3339 (e.g. 2025-01-01T00:00:00Z)");
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
