package com.modelengine.observability.config;

import com.modelengine.observability.service.AggregationType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dynamic loader for metrics definitions from configuration.
 * Supports hot-reloading via scheduled refresh.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsDefinitionLoader {

    private final ObservabilityProperties properties;

    @Getter
    private volatile List<MetricDefinition> definitions = Collections.emptyList();

    private volatile Map<String, MetricDefinition> definitionMap = Collections.emptyMap();

    @PostConstruct
    public void init() {
        loadDefinitions();
        log.info("Loaded {} metric definitions from configuration", definitions.size());
    }

    /**
     * Reloads metric definitions from configuration.
     * Called periodically by scheduler or can be triggered manually.
     */
    @Scheduled(fixedDelayString = "${observability.metrics.refresh-interval:60000}")
    public void loadDefinitions() {
        List<ObservabilityProperties.Metrics.MetricDefinition> configDefs = properties.getMetrics().getDefinitions();

        if (configDefs == null || configDefs.isEmpty()) {
            log.warn("No metric definitions found in configuration");
            this.definitions = Collections.emptyList();
            this.definitionMap = Collections.emptyMap();
            return;
        }

        List<MetricDefinition> loaded = configDefs.stream()
                .map(this::convert)
                .collect(Collectors.toList());

        Map<String, MetricDefinition> map = loaded.stream()
                .collect(Collectors.toMap(MetricDefinition::metricName, d -> d));

        int previousCount = this.definitions.size();
        this.definitions = Collections.unmodifiableList(loaded);
        this.definitionMap = Collections.unmodifiableMap(map);

        if (previousCount != loaded.size()) {
            log.info("Metric definitions reloaded: {} definitions (was {})", loaded.size(), previousCount);
        } else {
            log.debug("Metric definitions refreshed: {} definitions", loaded.size());
        }
    }

    /**
     * Gets a metric definition by name.
     */
    public MetricDefinition getDefinition(String metricName) {
        return definitionMap.get(metricName);
    }

    /**
     * Checks if a metric definition exists.
     */
    public boolean hasDefinition(String metricName) {
        return definitionMap.containsKey(metricName);
    }

    private MetricDefinition convert(ObservabilityProperties.Metrics.MetricDefinition config) {
        return new MetricDefinition(
                config.getMetricName(),
                config.getPromqlTemplate(),
                config.getDisplayName(),
                config.getUnit(),
                parseAggregationType(config.getAggregationType())
        );
    }

    private AggregationType parseAggregationType(String type) {
        if (type == null || type.isEmpty()) {
            return AggregationType.AVG;
        }
        try {
            return AggregationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown aggregation type '{}', defaulting to AVG", type);
            return AggregationType.AVG;
        }
    }

    /**
     * Runtime metric definition with parsed aggregation type.
     */
    public record MetricDefinition(
            String metricName,
            String promqlTemplate,
            String displayName,
            String unit,
            AggregationType aggregationType
    ) {
    }
}
