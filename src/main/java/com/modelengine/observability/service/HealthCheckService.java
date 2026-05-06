package com.modelengine.observability.service;

import com.modelengine.observability.client.PrometheusClient;
import com.modelengine.observability.client.K8sClient;
import com.modelengine.observability.config.ObservabilityProperties;
import com.modelengine.observability.dto.HealthStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for checking the health of the observability module and its dependencies.
 * <p>
 * Performs connectivity checks against Prometheus and Kubernetes API,
 * returning a comprehensive health status report with flat component status strings.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final PrometheusClient prometheusClient;
    private final K8sClient k8sClient;
    private final ObservabilityProperties properties;

    private static final String STATUS_HEALTHY = "healthy";
    private static final String STATUS_UNHEALTHY = "unhealthy";
    private static final String CONNECTED = "connected";
    private static final String DISCONNECTED = "disconnected";
    private static final String COMPONENT_PROMETHEUS = "prometheus";
    private static final String COMPONENT_KUBERNETES = "kubernetes";

    /**
     * Performs health checks against all dependencies and returns the aggregated health status.
     *
     * @return {@link HealthStatusDTO} containing overall status and per-component details
     */
    public HealthStatusDTO checkHealth() {
        log.debug("Performing health checks");

        Map<String, String> components = new LinkedHashMap<>();

        // Check Prometheus connectivity
        boolean prometheusHealthy = checkPrometheus();
        components.put(COMPONENT_PROMETHEUS, prometheusHealthy ? CONNECTED : DISCONNECTED);

        // Check K8s API connectivity
        boolean k8sHealthy = checkKubernetes();
        components.put(COMPONENT_KUBERNETES, k8sHealthy ? CONNECTED : DISCONNECTED);

        // Overall status: healthy only if all components are connected
        String overallStatus = (prometheusHealthy && k8sHealthy) ? STATUS_HEALTHY : STATUS_UNHEALTHY;

        HealthStatusDTO healthStatus = HealthStatusDTO.builder()
                .status(overallStatus)
                .version(properties.getMeVersion())
                .timestamp(Instant.now())
                .clusterId(properties.getClusterId())
                .meVersion(properties.getMeVersion())
                .components(components)
                .build();

        log.info("Health check completed: status={}, prometheus={}, kubernetes={}",
                overallStatus, components.get(COMPONENT_PROMETHEUS), components.get(COMPONENT_KUBERNETES));

        return healthStatus;
    }

    /**
     * Checks Prometheus connectivity by sending a ping request.
     *
     * @return true if Prometheus is reachable, false otherwise
     */
    private boolean checkPrometheus() {
        try {
            boolean pingResult = prometheusClient.ping();
            log.debug("Prometheus ping result: {}", pingResult);
            return pingResult;
        } catch (Exception e) {
            log.warn("Prometheus connectivity check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks Kubernetes API connectivity by sending a ping request.
     *
     * @return true if K8s API is reachable, false otherwise
     */
    private boolean checkKubernetes() {
        try {
            boolean pingResult = k8sClient.ping();
            log.debug("Kubernetes API ping result: {}", pingResult);
            return pingResult;
        } catch (Exception e) {
            log.warn("Kubernetes API connectivity check failed: {}", e.getMessage());
            return false;
        }
    }
}
