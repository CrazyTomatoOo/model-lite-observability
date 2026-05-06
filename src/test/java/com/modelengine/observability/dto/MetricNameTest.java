package com.modelengine.observability.dto;

import com.modelengine.observability.config.MetricsDefinitionLoader;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for MetricName enum and its validation integration with MetricsRangeQueryDTO.
 */
class MetricNameTest {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;
    private static MetricsDefinitionLoader mockLoader;

    @BeforeAll
    static void setUp() {
        // Create a mocked MetricsDefinitionLoader that delegates to MetricName enum
        mockLoader = mock(MetricsDefinitionLoader.class);
        when(mockLoader.hasDefinition(anyString()))
                .thenAnswer(invocation -> MetricName.isValid(invocation.getArgument(0)));

        // Create MetricsValidator with the mock — simulates Spring injection
        MetricsValidator metricsValidator = new MetricsValidator(mockLoader);

        // Build a ValidatorFactory with a custom ConstraintValidatorFactory
        // that returns our manually-instantiated MetricsValidator
        validatorFactory = Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        if (key == MetricsValidator.class) {
                            @SuppressWarnings("unchecked")
                            T cast = (T) metricsValidator;
                            return cast;
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to instantiate " + key, e);
                        }
                    }
                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {}
                })
                .buildValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    // ─── MetricName enum exists for all 11 values ───

    @Test
    void allElevenMetricNamesExist() {
        MetricName[] values = MetricName.values();
        assertEquals(11, values.length, "There must be exactly 11 metric names");

        assertNotNull(MetricName.valueOf("TTFT"));
        assertNotNull(MetricName.valueOf("TPOT"));
        assertNotNull(MetricName.valueOf("GENERATION_TOKENS"));
        assertNotNull(MetricName.valueOf("PROMPT_THROUGHPUT"));
        assertNotNull(MetricName.valueOf("DECODE_THROUGHPUT"));
        assertNotNull(MetricName.valueOf("QPS"));
        assertNotNull(MetricName.valueOf("CONNECTIONS"));
        assertNotNull(MetricName.valueOf("SUCCESS_RATE"));
        assertNotNull(MetricName.valueOf("TOTAL_REQUESTS"));
        assertNotNull(MetricName.valueOf("FAILED_REQUESTS"));
        assertNotNull(MetricName.valueOf("SUCCESS_REQUESTS"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ttft", "tpot", "generation_tokens", "prompt_throughput",
            "decode_throughput", "qps", "connections", "success_rate",
            "total_requests", "failed_requests", "success_requests"
    })
    void allMetricValuesAreValid(String metricValue) {
        assertTrue(MetricName.isValid(metricValue));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ttft", "tpot", "generation_tokens", "prompt_throughput",
            "decode_throughput", "qps", "connections", "success_rate",
            "total_requests", "failed_requests", "success_requests"
    })
    void fromValueReturnsCorrectEnum(String metricValue) {
        assertNotNull(MetricName.fromValue(metricValue));
    }

    @Test
    void eachEnumHasCorrectValue() {
        assertEquals("ttft", MetricName.TTFT.getValue());
        assertEquals("tpot", MetricName.TPOT.getValue());
        assertEquals("generation_tokens", MetricName.GENERATION_TOKENS.getValue());
        assertEquals("prompt_throughput", MetricName.PROMPT_THROUGHPUT.getValue());
        assertEquals("decode_throughput", MetricName.DECODE_THROUGHPUT.getValue());
        assertEquals("qps", MetricName.QPS.getValue());
        assertEquals("connections", MetricName.CONNECTIONS.getValue());
        assertEquals("success_rate", MetricName.SUCCESS_RATE.getValue());
        assertEquals("total_requests", MetricName.TOTAL_REQUESTS.getValue());
        assertEquals("failed_requests", MetricName.FAILED_REQUESTS.getValue());
        assertEquals("success_requests", MetricName.SUCCESS_REQUESTS.getValue());
    }

    // ─── Invalid metric names are rejected ───

    @Test
    void fromValueThrowsForInvalidMetric() {
        assertThrows(IllegalArgumentException.class, () -> MetricName.fromValue("invalid_metric"));
        assertThrows(IllegalArgumentException.class, () -> MetricName.fromValue(""));
        assertThrows(IllegalArgumentException.class, () -> MetricName.fromValue("ttft "));
        assertThrows(IllegalArgumentException.class, () -> MetricName.fromValue("tpot_typo"));
    }

    @Test
    void isValidReturnsFalseForInvalidMetrics() {
        assertFalse(MetricName.isValid("invalid_metric"));
        assertFalse(MetricName.isValid(""));
        assertFalse(MetricName.isValid("ttft "));
        assertFalse(MetricName.isValid("GENERATION_TOKENS"));
    }

    // ─── Validation on MetricsRangeQueryDTO via MetricsDefinitionLoader ───

    @Test
    void nullMetricsPassesValidation() {
        MetricsRangeQueryDTO dto = MetricsRangeQueryDTO.builder()
                .metrics(null)
                .startTime("2025-01-01T00:00:00Z")
                .endTime("2025-01-02T00:00:00Z")
                .step("1m")
                .build();

        Set<ConstraintViolation<MetricsRangeQueryDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Null metrics should pass validation");
    }

    @Test
    void emptyMetricsListPassesValidation() {
        MetricsRangeQueryDTO dto = MetricsRangeQueryDTO.builder()
                .metrics(List.of())
                .startTime("2025-01-01T00:00:00Z")
                .endTime("2025-01-02T00:00:00Z")
                .step("1m")
                .build();

        Set<ConstraintViolation<MetricsRangeQueryDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Empty metrics list should pass validation");
    }

    @Test
    void allValidMetricsPassesValidation() {
        MetricsRangeQueryDTO dto = MetricsRangeQueryDTO.builder()
                .metrics(List.of(
                        "ttft", "tpot", "generation_tokens", "prompt_throughput",
                        "decode_throughput", "qps", "connections", "success_rate",
                        "total_requests", "failed_requests", "success_requests"
                ))
                .startTime("2025-01-01T00:00:00Z")
                .endTime("2025-01-02T00:00:00Z")
                .step("1m")
                .build();

        Set<ConstraintViolation<MetricsRangeQueryDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "All valid metrics should pass validation");
    }

    @Test
    void invalidMetricNameFailsValidation() {
        MetricsRangeQueryDTO dto = MetricsRangeQueryDTO.builder()
                .metrics(List.of("ttft", "invalid_metric"))
                .startTime("2025-01-01T00:00:00Z")
                .endTime("2025-01-02T00:00:00Z")
                .step("1m")
                .build();

        Set<ConstraintViolation<MetricsRangeQueryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Invalid metric name should fail validation");
    }

    @Test
    void mixedValidAndInvalidFailsValidation() {
        MetricsRangeQueryDTO dto = MetricsRangeQueryDTO.builder()
                .metrics(List.of("ttft", "qps", "bogus"))
                .startTime("2025-01-01T00:00:00Z")
                .endTime("2025-01-02T00:00:00Z")
                .step("1m")
                .build();

        Set<ConstraintViolation<MetricsRangeQueryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Mixed valid and invalid metrics should fail validation");
    }
}
