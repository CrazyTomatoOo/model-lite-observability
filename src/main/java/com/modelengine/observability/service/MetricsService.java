package com.modelengine.observability.service;

import com.modelengine.observability.cache.CacheManager;
import com.modelengine.observability.client.PrometheusClient;
import com.modelengine.observability.client.dto.PrometheusResponse;
import com.modelengine.observability.client.dto.PrometheusResult;
import com.modelengine.observability.config.MetricsDefinitionLoader;
import com.modelengine.observability.config.ObservabilityProperties;
import com.modelengine.observability.dto.DataPointDTO;
import com.modelengine.observability.dto.MetricSeriesDTO;
import com.modelengine.observability.dto.MetricsRangeQueryDTO;
import com.modelengine.observability.dto.MetricsRangeResponseDTO;
import com.modelengine.observability.util.StepParser;
import com.modelengine.observability.util.TimeParser;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for querying and aggregating model service performance metrics from Prometheus.
 * Metrics definitions are loaded dynamically from configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final PrometheusClient prometheusClient;
    private final TopologyService topologyService;
    private final MetricsAggregator metricsAggregator;
    @Qualifier("observabilityCacheManager")
    private final CacheManager cacheManager;
    private final ObservabilityProperties properties;
    private final MetricsDefinitionLoader metricsDefinitionLoader;

    private static final String CACHE_KEY_METRICS_PREFIX = "metrics:";
    private static final String CACHE_KEY_METRICS_RANGE_PREFIX = "metrics:range:";

    public List<MetricSeriesDTO> getServiceMetrics(String instanceName) {
        log.debug("Getting real-time metrics for service: {}", instanceName);

        String cacheKey = CACHE_KEY_METRICS_PREFIX + instanceName;
        return cacheManager.getOrLoad(
                cacheKey,
                () -> loadServiceMetrics(instanceName),
                properties.getCache().getMetricsTtl()
        );
    }

    public MetricsRangeResponseDTO getServiceMetricsRange(String instanceName, MetricsRangeQueryDTO query) {
        log.debug("Getting range metrics for service: {}, query: {}", instanceName, query);

        String cacheKey = buildRangeCacheKey(instanceName, query);
        return cacheManager.getOrLoad(
                cacheKey,
                () -> loadServiceMetricsRange(instanceName, query),
                properties.getCache().getMetricsTtl()
        );
    }

    private List<MetricSeriesDTO> loadServiceMetrics(String instanceName) {
        List<Pod> pods = topologyService.getPodsForInstance(instanceName);
        if (pods.isEmpty()) {
            log.warn("No pods found for service: {}", instanceName);
            return List.of();
        }

        List<MetricSeriesDTO> serviceMetrics = new ArrayList<>();
        List<MetricsDefinitionLoader.MetricDefinition> definitions = metricsDefinitionLoader.getDefinitions();

        if (definitions.isEmpty()) {
            log.warn("No metric definitions configured");
            return List.of();
        }

        for (MetricsDefinitionLoader.MetricDefinition def : definitions) {
            List<MetricSeriesDTO> podMetrics = new ArrayList<>();

            for (Pod pod : pods) {
                String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "";
                if (podName.isEmpty()) {
                    continue;
                }

                String promQL = String.format(def.promqlTemplate(), podName);
                MetricSeriesDTO series = queryInstantMetric(promQL, def, podName);
                if (series != null) {
                    podMetrics.add(series);
                }
            }

            if (!podMetrics.isEmpty()) {
                MetricSeriesDTO aggregated = metricsAggregator.aggregate(podMetrics, def.aggregationType());
                if (aggregated != null) {
                    serviceMetrics.add(aggregated);
                }
            }
        }

        log.debug("Loaded {} metrics for service {}", serviceMetrics.size(), instanceName);
        return serviceMetrics;
    }

    private MetricsRangeResponseDTO loadServiceMetricsRange(String instanceName, MetricsRangeQueryDTO query) {
        List<Pod> pods = topologyService.getPodsForInstance(instanceName);
        if (pods.isEmpty()) {
            log.warn("No pods found for service: {}", instanceName);
            return buildEmptyRangeResponse(instanceName, query);
        }

        Instant start = TimeParser.parseTime(query.getStartTime());
        Instant end = TimeParser.parseTime(query.getEndTime());
        Duration step = StepParser.parseStep(query.getStep());
        Duration timeout = parseTimeout(query.getTimeout());

        List<String> requestedMetrics = query.getMetrics();
        List<MetricsDefinitionLoader.MetricDefinition> defsToQuery = metricsDefinitionLoader.getDefinitions().stream()
                .filter(def -> requestedMetrics == null || requestedMetrics.isEmpty() || requestedMetrics.contains(def.metricName()))
                .toList();

        List<MetricSeriesDTO> serviceMetrics = new ArrayList<>();

        for (MetricsDefinitionLoader.MetricDefinition def : defsToQuery) {
            List<MetricSeriesDTO> podMetrics = new ArrayList<>();

            for (Pod pod : pods) {
                String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "";
                if (podName.isEmpty()) {
                    continue;
                }

                String promQL = String.format(def.promqlTemplate(), podName);
                MetricSeriesDTO series = queryRangeMetric(promQL, def, podName, start, end, step, timeout);
                if (series != null) {
                    podMetrics.add(series);
                }
            }

            if (!podMetrics.isEmpty()) {
                MetricSeriesDTO aggregated = metricsAggregator.aggregate(podMetrics, def.aggregationType());
                if (aggregated != null) {
                    serviceMetrics.add(aggregated);
                }
            }
        }

        // Apply limit: if limit > 0, truncate dataPoints in each metric series
        int maxPoints = query.getLimit() != null ? query.getLimit() : 0;
        if (maxPoints > 0) {
            serviceMetrics = applyLimit(serviceMetrics, maxPoints);
        }

        // Determine step string representation for response
        String stepStr = query.getStep() != null ? query.getStep().toString() : step.toSeconds() + "s";

        return MetricsRangeResponseDTO.builder()
                .instanceName(instanceName)
                .startTime(start)
                .endTime(end)
                .step(stepStr)
                .metrics(serviceMetrics)
                .build();
    }

    private List<MetricSeriesDTO> applyLimit(List<MetricSeriesDTO> metrics, int maxPoints) {
        return metrics.stream()
                .map(series -> {
                    if (series.getDataPoints() == null || series.getDataPoints().size() <= maxPoints) {
                        return series;
                    }
                    return MetricSeriesDTO.builder()
                            .metricName(series.getMetricName())
                            .displayName(series.getDisplayName())
                            .unit(series.getUnit())
                            .aggregation(series.getAggregation())
                            .dataPoints(new ArrayList<>(series.getDataPoints().subList(0, maxPoints)))
                            .build();
                })
                .toList();
    }

    private MetricSeriesDTO queryInstantMetric(String promQL, MetricsDefinitionLoader.MetricDefinition definition, String podName) {
        try {
            PrometheusResponse response = prometheusClient.query(promQL);
            if (!response.isSuccess() || !response.hasData()) {
                log.debug("No data for metric {} on pod {}", definition.metricName(), podName);
                return null;
            }

            List<DataPointDTO> dataPoints = new ArrayList<>();
            for (PrometheusResult result : response.getData().getResult()) {
                Double value = extractValue(result.getValue());
                if (value != null) {
                    dataPoints.add(DataPointDTO.builder()
                            .timestamp(Instant.now())
                            .value(value)
                            .build());
                }
            }

            if (dataPoints.isEmpty()) {
                return null;
            }

            return MetricSeriesDTO.builder()
                    .metricName(definition.metricName())
                    .displayName(definition.displayName())
                    .unit(definition.unit())
                    .aggregation(definition.aggregationType().name().toLowerCase())
                    .dataPoints(dataPoints)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to query metric {} for pod {}: {}", definition.metricName(), podName, e.getMessage());
            return null;
        }
    }

    private MetricSeriesDTO queryRangeMetric(
            String promQL,
            MetricsDefinitionLoader.MetricDefinition definition,
            String podName,
            Instant start,
            Instant end,
            Duration step,
            Duration timeout
    ) {
        try {
            PrometheusResponse response = prometheusClient.queryRange(promQL, start, end, step, timeout);
            if (!response.isSuccess() || !response.hasData()) {
                log.debug("No range data for metric {} on pod {}", definition.metricName(), podName);
                return null;
            }

            List<DataPointDTO> dataPoints = new ArrayList<>();
            for (PrometheusResult result : response.getData().getResult()) {
                List<DataPointDTO> points = extractValues(result.getValues());
                dataPoints.addAll(points);
            }

            if (dataPoints.isEmpty()) {
                return null;
            }

            dataPoints.sort(Comparator.comparing(DataPointDTO::getTimestamp));

            return MetricSeriesDTO.builder()
                    .metricName(definition.metricName())
                    .displayName(definition.displayName())
                    .unit(definition.unit())
                    .aggregation(definition.aggregationType().name().toLowerCase())
                    .dataPoints(dataPoints)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to query range metric {} for pod {}: {}", definition.metricName(), podName, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Double extractValue(Object valueObj) {
        if (valueObj instanceof List<?> valueList && valueList.size() >= 2) {
            try {
                return Double.parseDouble(valueList.get(1).toString());
            } catch (NumberFormatException e) {
                log.debug("Failed to parse value: {}", valueList.get(1));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<DataPointDTO> extractValues(Object valuesObj) {
        List<DataPointDTO> dataPoints = new ArrayList<>();
        if (valuesObj instanceof List<?> valuesList) {
            for (Object item : valuesList) {
                if (item instanceof List<?> pair && pair.size() >= 2) {
                    try {
                        long epochSeconds = Long.parseLong(pair.get(0).toString());
                        double value = Double.parseDouble(pair.get(1).toString());
                        dataPoints.add(DataPointDTO.builder()
                                .timestamp(Instant.ofEpochSecond(epochSeconds))
                                .value(value)
                                .build());
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        log.debug("Failed to parse data point: {}", item);
                    }
                }
            }
        }
        return dataPoints;
    }

    private MetricsRangeResponseDTO buildEmptyRangeResponse(String instanceName, MetricsRangeQueryDTO query) {
        Instant start;
        Instant end;
        Duration step;
        try {
            start = TimeParser.parseTime(query.getStartTime());
            end = TimeParser.parseTime(query.getEndTime());
            step = StepParser.parseStep(query.getStep());
        } catch (Exception e) {
            start = Instant.now();
            end = Instant.now();
            step = Duration.ofMinutes(5);
        }

        String stepStr = query.getStep() != null ? query.getStep().toString() : step.toSeconds() + "s";

        return MetricsRangeResponseDTO.builder()
                .instanceName(instanceName)
                .startTime(start)
                .endTime(end)
                .step(stepStr)
                .metrics(List.of())
                .build();
    }

    private String buildRangeCacheKey(String instanceName, MetricsRangeQueryDTO query) {
        return CACHE_KEY_METRICS_RANGE_PREFIX + instanceName + ":"
                + query.getStartTime() + ":"
                + query.getEndTime() + ":"
                + query.getStep() + ":"
                + query.getTimeout() + ":"
                + query.getLimit() + ":"
                + (query.getMetrics() != null ? String.join(",", query.getMetrics()) : "all");
    }

    private Duration parseTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.isBlank()) {
            return Duration.ofSeconds(30);
        }
        try {
            return Duration.parse("PT" + timeoutStr.trim().toUpperCase());
        } catch (Exception e) {
            log.warn("Failed to parse timeout '{}', using default 30s", timeoutStr);
            return Duration.ofSeconds(30);
        }
    }

    public void invalidateCache(String instanceName) {
        log.debug("Invalidating metrics cache for service: {}", instanceName);
        cacheManager.invalidate(CACHE_KEY_METRICS_PREFIX + instanceName);
    }
}
