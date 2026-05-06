package com.modelengine.observability.service;

import com.modelengine.observability.client.K8sClient;
import com.modelengine.observability.client.PrometheusClient;
import com.modelengine.observability.config.ObservabilityProperties;
import com.modelengine.observability.dto.HealthStatusDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

    @Mock
    private PrometheusClient prometheusClient;

    @Mock
    private K8sClient k8sClient;

    @Mock
    private ObservabilityProperties properties;

    @InjectMocks
    private HealthCheckService healthCheckService;

    @Test
    void checkHealthAllHealthy() {
        when(prometheusClient.ping()).thenReturn(true);
        when(k8sClient.ping()).thenReturn(true);
        when(properties.getClusterId()).thenReturn("test-cluster");
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("healthy", result.getStatus());
        assertEquals("test-cluster", result.getClusterId());
        assertEquals("2.0.0", result.getVersion());
        assertEquals("2.0.0", result.getMeVersion());
        assertNotNull(result.getTimestamp());
        assertEquals("connected", result.getComponents().get("prometheus"));
        assertEquals("connected", result.getComponents().get("kubernetes"));
    }

    @Test
    void checkHealthPrometheusDown() {
        when(prometheusClient.ping()).thenReturn(false);
        when(k8sClient.ping()).thenReturn(true);
        when(properties.getClusterId()).thenReturn("test-cluster");
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("unhealthy", result.getStatus());
        assertEquals("disconnected", result.getComponents().get("prometheus"));
        assertEquals("connected", result.getComponents().get("kubernetes"));
    }

    @Test
    void checkHealthK8sDown() {
        when(prometheusClient.ping()).thenReturn(true);
        when(k8sClient.ping()).thenReturn(false);
        when(properties.getClusterId()).thenReturn("test-cluster");
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("unhealthy", result.getStatus());
        assertEquals("connected", result.getComponents().get("prometheus"));
        assertEquals("disconnected", result.getComponents().get("kubernetes"));
    }

    @Test
    void checkHealthAllDown() {
        when(prometheusClient.ping()).thenReturn(false);
        when(k8sClient.ping()).thenReturn(false);
        when(properties.getClusterId()).thenReturn("test-cluster");
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("unhealthy", result.getStatus());
        assertEquals("disconnected", result.getComponents().get("prometheus"));
        assertEquals("disconnected", result.getComponents().get("kubernetes"));
    }

    @Test
    void componentsAreFlatStrings() {
        when(prometheusClient.ping()).thenReturn(true);
        when(k8sClient.ping()).thenReturn(true);
        when(properties.getClusterId()).thenReturn("test-cluster");
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        for (String value : result.getComponents().values()) {
            assertInstanceOf(String.class, value, "component value must be a flat String, not a nested object");
        }
    }

    @Test
    void checkHealthHasClusterIdAndMeVersion() {
        when(prometheusClient.ping()).thenReturn(true);
        when(k8sClient.ping()).thenReturn(true);
        when(properties.getClusterId()).thenReturn("prod-cluster-01");
        when(properties.getMeVersion()).thenReturn("3.1.4");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertEquals("prod-cluster-01", result.getClusterId());
        assertEquals("3.1.4", result.getMeVersion());
    }

    @Test
    void checkHealthIncludesTimestamp() {
        when(prometheusClient.ping()).thenReturn(true);
        when(k8sClient.ping()).thenReturn(true);
        when(properties.getClusterId()).thenReturn("test-cluster");
        when(properties.getMeVersion()).thenReturn("2.0.0");

        HealthStatusDTO result = healthCheckService.checkHealth();

        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isAfter(java.time.Instant.EPOCH));
    }
}
