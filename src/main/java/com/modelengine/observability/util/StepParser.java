package com.modelengine.observability.util;

import com.modelengine.observability.exception.ObservabilityException;

import java.time.Duration;

/**
 * Parses step values from either Duration strings or numeric seconds.
 */
public final class StepParser {

    private StepParser() {
        // utility class
    }

    /**
     * Parses an Object to {@link Duration}.
     * <ul>
     *   <li>{@link String} → parsed as a Duration expression (e.g. "60s", "5m", "1h")</li>
     *   <li>{@link Number} → treated as seconds via {@link Duration#ofSeconds(long)}</li>
     *   <li>Any other type or parse failure → {@link ObservabilityException} with BAD_REQUEST</li>
     * </ul>
     *
     * @param raw the raw value (String or Number)
     * @return the parsed Duration
     * @throws ObservabilityException if the value cannot be parsed
     */
    public static Duration parseStep(Object raw) {
        if (raw == null) {
            return Duration.ofMinutes(5); // default step
        }

        if (raw instanceof String s) {
            return parseDurationString(s);
        }

        if (raw instanceof Number n) {
            long seconds = n.longValue();
            if (seconds <= 0) {
                throw ObservabilityException.badRequest(
                        "Step must be positive, got " + seconds);
            }
            return Duration.ofSeconds(seconds);
        }

        throw ObservabilityException.badRequest(
                "step must be a string (e.g. '60s', '5m', '1h') or a number (seconds), got "
                        + raw.getClass().getSimpleName());
    }

    private static Duration parseDurationString(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return Duration.ofMinutes(5);
        }

        // Try ISO-8601 Duration format: PT60S, PT5M, PT1H, etc.
        try {
            return Duration.parse("PT" + trimmed.toUpperCase());
        } catch (Exception ignored) {
            // try standard Duration.parse (for full ISO-8601 like PT5M)
        }

        try {
            return Duration.parse(trimmed);
        } catch (Exception ignored) {
            // fall through to custom parsing
        }

        // Custom parsing for simple "<number><unit>" format: 60s, 5m, 1h, 2d
        try {
            char lastChar = trimmed.charAt(trimmed.length() - 1);
            if (Character.isDigit(lastChar)) {
                // pure number string: treat as seconds
                long seconds = Long.parseLong(trimmed);
                if (seconds <= 0) {
                    throw ObservabilityException.badRequest("Step must be positive, got " + seconds);
                }
                return Duration.ofSeconds(seconds);
            }

            long value = Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
            if (value <= 0) {
                throw ObservabilityException.badRequest("Step must be positive");
            }

            switch (lastChar) {
                case 's':
                case 'S':
                    return Duration.ofSeconds(value);
                case 'm':
                    return Duration.ofMinutes(value);
                case 'h':
                case 'H':
                    return Duration.ofHours(value);
                case 'd':
                case 'D':
                    return Duration.ofDays(value);
                default:
                    throw ObservabilityException.badRequest(
                            "Invalid step unit '" + lastChar + "'. Expected s, m, h, or d");
            }
        } catch (NumberFormatException e) {
            throw ObservabilityException.badRequest(
                    "Invalid step format: '" + trimmed + "'. Expected format like '60s', '5m', '1h'");
        }
    }
}
