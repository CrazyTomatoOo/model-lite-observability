package com.modelengine.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the observability module.
 * Maps to properties prefixed with "observability" in application.yml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /** Prometheus client configuration */
    private Prometheus prometheus = new Prometheus();

    /** Cache configuration */
    private Cache cache = new Cache();

    /** Kubernetes client configuration */
    private Kubernetes kubernetes = new Kubernetes();

    /** Metrics configuration */
    /** Metrics configuration */
    private Metrics metrics = new Metrics();

    /** Cluster identifier */
    private String clusterId = "";

    /** Model engine version */
    private String meVersion = "";

    @Data
    public static class Prometheus {
        /** Prometheus server URL */
        private String url = "http://prometheus:9090";

        /** Query timeout */
        private Duration timeout = Duration.ofSeconds(30);

        /** Maximum retry attempts */
        private int maxRetries = 3;

        /** Retry backoff duration */
        private Duration retryBackoff = Duration.ofSeconds(1);
    }

    @Data
    public static class Cache {
        /** Metrics cache time-to-live */
        private Duration metricsTtl = Duration.ofMinutes(5);

        /** Service list cache time-to-live */
        private Duration servicesTtl = Duration.ofMinutes(5);

        /** Maximum cache size */
        private int maxSize = 1000;
    }

    @Data
    public static class Kubernetes {
        /** Whether to use in-cluster config */
        private boolean inCluster = true;

        /** Path to kubeconfig file (empty = use in-cluster or default) */
        private String configPath = "";

        /** Namespace to watch (empty = all namespaces) */
        private String namespace = "";

        /** Connection timeout */
        private Duration connectionTimeout = Duration.ofSeconds(10);

        /** Request timeout */
        private Duration requestTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Metrics {
        /** Metric definitions */
        private List<MetricDefinition> definitions = new ArrayList<>();
        /** Configuration refresh interval */
        private Duration refreshInterval = Duration.ofMinutes(1);

        @Data
        public static class MetricDefinition {
            /** Metric identifier */
            private String metricName;
            /** PromQL query template with %s placeholder for pod name */
            private String promqlTemplate;
            /** Human-readable display name */
            private String displayName;
            /** Unit of measurement */
            private String unit;
            /** Aggregation type: AVG, SUM, WEIGHTED_AVG */
            private String aggregationType = "AVG";
        }
    }
}
