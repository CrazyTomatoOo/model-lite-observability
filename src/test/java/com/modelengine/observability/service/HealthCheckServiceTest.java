package com.modelengine.observability.service;

import com.modelengine.observability.config.ObservabilityProperties;
import com.modelengine.observability.dto.HealthStatusDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

    @Mock
    private ObservabilityProperties properties;

    private HealthCheckService healthCheckService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        healthCheckService = new HealthCheckService(properties);
    }
    @Test
    void checkHealthAllHealthy() {
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("healthy", result.getStatus());
        assertEquals("9e9be3cf5a3440988f90d06c958430ae", result.getClusterId());
        assertEquals("2.0.0", result.getVersion());
        assertEquals("2.0.0", result.getMeVersion());
        assertNotNull(result.getTimestamp());
        assertEquals("connected", result.getComponents().get("prometheus"));
        assertEquals("connected", result.getComponents().get("kubernetes"));
    }

    @Test
    void checkHealthReturnsHealthyAlways() {
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("healthy", result.getStatus());
        assertEquals("connected", result.getComponents().get("prometheus"));
        assertEquals("connected", result.getComponents().get("kubernetes"));
    }
    @Test
    void checkHealthAlwaysConnected() {
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("healthy", result.getStatus());
        assertEquals("connected", result.getComponents().get("prometheus"));
        assertEquals("connected", result.getComponents().get("kubernetes"));
    }
    @Test
    void checkHealthAlwaysReturnsConnected() {
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("healthy", result.getStatus());
        assertEquals("connected", result.getComponents().get("prometheus"));
        assertEquals("connected", result.getComponents().get("kubernetes"));
    }
    @Test
    void componentsAreFlatStrings() {
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        for (String value : result.getComponents().values()) {
            assertInstanceOf(String.class, value, "component value must be a flat String, not a nested object");
        }
    }
    @Test
    void checkHealthHasClusterIdAndMeVersion() {
        when(properties.getMeVersion()).thenReturn("3.1.4");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("9e9be3cf5a3440988f90d06c958430ae", result.getClusterId());
        assertEquals("3.1.4", result.getMeVersion());
    }
    @Test
    void checkHealthIncludesTimestamp() {
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isAfter(java.time.Instant.EPOCH));
    }
}
