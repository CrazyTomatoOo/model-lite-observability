package com.modelengine.observability.util;

import com.modelengine.observability.exception.ObservabilityException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StepParserTest {

    @Test
    void parseDurationString60s() {
        Duration result = StepParser.parseStep("60s");
        assertEquals(Duration.ofSeconds(60), result);
    }

    @Test
    void parseDurationString5m() {
        Duration result = StepParser.parseStep("5m");
        assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    void parseDurationString1h() {
        Duration result = StepParser.parseStep("1h");
        assertEquals(Duration.ofHours(1), result);
    }

    @Test
    void parseDurationString2d() {
        Duration result = StepParser.parseStep("2d");
        assertEquals(Duration.ofDays(2), result);
    }

    @Test
    void parseDurationStringUppercase() {
        Duration result = StepParser.parseStep("60S");
        assertEquals(Duration.ofSeconds(60), result);
    }

    @Test
    void parseDurationString5M() {
        Duration result = StepParser.parseStep("5M");
        assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    void parseDurationString1H() {
        Duration result = StepParser.parseStep("1H");
        assertEquals(Duration.ofHours(1), result);
    }

    @Test
    void parseNumberAsSeconds() {
        Duration result = StepParser.parseStep(60);
        assertEquals(Duration.ofSeconds(60), result);
    }

    @Test
    void parseLongNumberAsSeconds() {
        Duration result = StepParser.parseStep(300L);
        assertEquals(Duration.ofSeconds(300), result);
    }

    @Test
    void parseDoubleNumberAsSeconds() {
        Duration result = StepParser.parseStep(120.0);
        assertEquals(Duration.ofSeconds(120), result);
    }

    @Test
    void nullStepReturnsDefault() {
        Duration result = StepParser.parseStep(null);
        assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    void emptyStringReturnsDefault() {
        Duration result = StepParser.parseStep("");
        assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    void zeroNumberThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> StepParser.parseStep(0));
        assertTrue(ex.getMessage().contains("Step must be positive"));
    }

    @Test
    void negativeNumberThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> StepParser.parseStep(-10));
        assertTrue(ex.getMessage().contains("Step must be positive"));
    }

    @Test
    void invalidStringThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> StepParser.parseStep("abc"));
        assertTrue(ex.getMessage().contains("Invalid step format"));
    }

    @Test
    void invalidUnitThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> StepParser.parseStep("5x"));
        assertTrue(ex.getMessage().contains("Invalid step unit"));
    }

    @Test
    void invalidTypeThrowsBadRequest() {
        ObservabilityException ex = assertThrows(ObservabilityException.class, () -> StepParser.parseStep(true));
        assertTrue(ex.getMessage().contains("must be a string"));
    }

    @Test
    void parsePureNumberStringAsSeconds() {
        Duration result = StepParser.parseStep("300");
        assertEquals(Duration.ofSeconds(300), result);
    }

    @Test
    void iso8601FormatIsSupported() {
        // PT5M is valid ISO-8601
        Duration result = StepParser.parseStep("PT5M");
        assertEquals(Duration.ofMinutes(5), result);
    }
}
