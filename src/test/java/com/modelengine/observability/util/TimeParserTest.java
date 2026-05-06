package com.modelengine.observability.util;

import com.modelengine.observability.exception.ObservabilityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TimeParserTest {

    @Test
    void parseRfc3339String() {
        Instant result = TimeParser.parseTime("2025-01-23T09:00:00Z");
        assertEquals(Instant.parse("2025-01-23T09:00:00Z"), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2025-01-01T00:00:00Z",
            "2024-12-31T23:59:59Z",
            "2025-06-15T12:30:00Z"
    })
    void parseVariousRfc3339Strings(String input) {
        Instant result = TimeParser.parseTime(input);
        assertEquals(Instant.parse(input), result);
    }

    @Test
    void parseUnixTimestampNumber() {
        Instant result = TimeParser.parseTime(1737622800L);
        assertEquals(Instant.ofEpochSecond(1737622800), result);
    }

    @Test
    void parseUnixTimestampInteger() {
        Instant result = TimeParser.parseTime(1737622800);
        assertEquals(Instant.ofEpochSecond(1737622800), result);
    }

    @Test
    void parseUnixTimestampDouble() {
        Instant result = TimeParser.parseTime(1737622800.0);
        assertEquals(Instant.ofEpochSecond(1737622800), result);
    }

    @Test
    void parseUnixTimestampZero() {
        Instant result = TimeParser.parseTime(0);
        assertEquals(Instant.EPOCH, result);
    }

    @Test
    void nullInputThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> TimeParser.parseTime(null));
        assertTrue(ex.getMessage().contains("startTime and endTime are required"));
    }

    @Test
    void invalidStringThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> TimeParser.parseTime("not-a-date"));
        assertTrue(ex.getMessage().contains("Invalid startTime/endTime format"));
    }

    @Test
    void invalidTypeThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> TimeParser.parseTime(true));
        assertTrue(ex.getMessage().contains("must be a string (RFC 3339) or a number"));
    }

    @Test
    void negativeUnixTimestampThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> TimeParser.parseTime(-1L));
        assertTrue(ex.getMessage().contains("must be non-negative"));
    }
}
