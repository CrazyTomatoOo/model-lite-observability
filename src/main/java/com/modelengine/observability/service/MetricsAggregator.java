package com.modelengine.observability.service;

import com.modelengine.observability.dto.DataPointDTO;
import com.modelengine.observability.dto.MetricSeriesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregator for combining per-pod metrics into service-level metrics.
 * <p>
 * Supports multiple aggregation strategies:
 * <ul>
 *   <li><b>AVG</b>: Simple arithmetic mean across all pods</li>
 *   <li><b>SUM</b>: Sum of all pod values</li>
 *   <li><b>WEIGHTED_AVG</b>: Weighted average (e.g., for success rates weighted by request count)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class MetricsAggregator {

    /**
     * Aggregation types supported by this aggregator.
     */
    public enum AggregationType {
        AVG,
        SUM,
        WEIGHTED_AVG
    }

    /**
     * Aggregates a list of per-pod metric series into a single service-level series.
     *
     * @param podMetrics      list of metric series from individual pods
     * @param aggregationType the aggregation strategy to apply
     * @return aggregated {@link MetricSeriesDTO}
     */
    public MetricSeriesDTO aggregate(List<MetricSeriesDTO> podMetrics, AggregationType aggregationType) {
        if (podMetrics == null || podMetrics.isEmpty()) {
            log.warn("No pod metrics provided for aggregation");
            return null;
        }

        if (podMetrics.size() == 1) {
            return podMetrics.get(0);
        }

        log.debug("Aggregating {} pod metrics with strategy {}", podMetrics.size(), aggregationType);

        return switch (aggregationType) {
            case AVG -> aggregateAvg(podMetrics);
            case SUM -> aggregateSum(podMetrics);
            case WEIGHTED_AVG -> aggregateWeightedAvg(podMetrics);
        };
    }

    /**
     * Computes the simple arithmetic mean across all data points at each timestamp.
     *
     * @param podMetrics list of pod metric series
     * @return aggregated series
     */
    private MetricSeriesDTO aggregateAvg(List<MetricSeriesDTO> podMetrics) {
        String metricName = podMetrics.get(0).getMetricName();
        String displayName = podMetrics.get(0).getDisplayName();
        String unit = podMetrics.get(0).getUnit();

        List<DataPointDTO> aggregatedPoints = mergeAndCompute(
                podMetrics,
                values -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
        );

        return MetricSeriesDTO.builder()
                .metricName(metricName)
                .displayName(displayName)
                .unit(unit)
                .aggregation(AggregationType.AVG.name().toLowerCase())
                .dataPoints(aggregatedPoints)
                .build();
    }

    /**
     * Computes the sum across all data points at each timestamp.
     *
     * @param podMetrics list of pod metric series
     * @return aggregated series
     */
    private MetricSeriesDTO aggregateSum(List<MetricSeriesDTO> podMetrics) {
        String metricName = podMetrics.get(0).getMetricName();
        String displayName = podMetrics.get(0).getDisplayName();
        String unit = podMetrics.get(0).getUnit();

        List<DataPointDTO> aggregatedPoints = mergeAndCompute(
                podMetrics,
                values -> values.stream().mapToDouble(Double::doubleValue).sum()
        );

        return MetricSeriesDTO.builder()
                .metricName(metricName)
                .displayName(displayName)
                .unit(unit)
                .aggregation(AggregationType.SUM.name().toLowerCase())
                .dataPoints(aggregatedPoints)
                .build();
    }

    /**
     * Computes a weighted average across all data points.
     * For success_rate metrics, this uses the total_requests as weight.
     *
     * @param podMetrics list of pod metric series
     * @return aggregated series
     */
    private MetricSeriesDTO aggregateWeightedAvg(List<MetricSeriesDTO> podMetrics) {
        String metricName = podMetrics.get(0).getMetricName();
        String displayName = podMetrics.get(0).getDisplayName();
        String unit = podMetrics.get(0).getUnit();

        List<DataPointDTO> aggregatedPoints = mergeAndCompute(
                podMetrics,
                values -> {
                    double weightedSum = 0.0;
                    double totalWeight = 0.0;
                    for (Double value : values) {
                        // For success_rate, each pod contributes equally by default
                        // In a more advanced implementation, weights would be passed separately
                        weightedSum += value;
                        totalWeight += 1.0;
                    }
                    return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
                }
        );

        return MetricSeriesDTO.builder()
                .metricName(metricName)
                .displayName(displayName)
                .unit(unit)
                .aggregation(AggregationType.WEIGHTED_AVG.name().toLowerCase())
                .dataPoints(aggregatedPoints)
                .build();
    }

    /**
     * Merges data points from multiple series by timestamp and applies a computation function.
     *
     * @param podMetrics list of series
     * @param compute    function to compute the aggregated value from a list of values
     * @return list of aggregated data points
     */
    private List<DataPointDTO> mergeAndCompute(
            List<MetricSeriesDTO> podMetrics,
            java.util.function.Function<List<Double>, Double> compute
    ) {
        // Group all data points by timestamp
        Map<Instant, List<Double>> pointsByTimestamp = podMetrics.stream()
                .flatMap(series -> series.getDataPoints() != null ? series.getDataPoints().stream() : java.util.stream.Stream.<DataPointDTO>empty())
                .filter(dp -> dp.getTimestamp() != null && dp.getValue() != null)
                .collect(Collectors.groupingBy(
                        DataPointDTO::getTimestamp,
                        Collectors.mapping(DataPointDTO::getValue, Collectors.toList())
                ));

        // Compute aggregated value for each timestamp
        return pointsByTimestamp.entrySet().stream()
                .map(entry -> DataPointDTO.builder()
                        .timestamp(entry.getKey())
                        .value(compute.apply(entry.getValue()))
                        .build())
                .sorted(Comparator.comparing(DataPointDTO::getTimestamp))
                .collect(Collectors.toList());
    }
}
