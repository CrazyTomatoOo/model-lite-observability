package com.modelengine.observability.service;

import com.modelengine.observability.dto.DataPointDTO;
import com.modelengine.observability.dto.MetricSeriesDTO;
import com.modelengine.observability.service.MetricsAggregator.AggregationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsAggregatorTest {

    private final MetricsAggregator aggregator = new MetricsAggregator();

    @Test
    void aggregateAvg() {
        List<MetricSeriesDTO> podMetrics = List.of(
                createSeries("ttft", List.of(
                        createPoint(Instant.parse("2025-01-23T09:00:00Z"), 100.0),
                        createPoint(Instant.parse("2025-01-23T09:01:00Z"), 200.0)
                )),
                createSeries("ttft", List.of(
                        createPoint(Instant.parse("2025-01-23T09:00:00Z"), 200.0),
                        createPoint(Instant.parse("2025-01-23T09:01:00Z"), 400.0)
                ))
        );

        MetricSeriesDTO result = aggregator.aggregate(podMetrics, AggregationType.AVG);

        assertEquals("avg", result.getAggregation());
        assertEquals(2, result.getDataPoints().size());
        assertEquals(150.0, result.getDataPoints().get(0).getValue(), 0.01);
        assertEquals(300.0, result.getDataPoints().get(1).getValue(), 0.01);
    }

    @Test
    void aggregateSum() {
        List<MetricSeriesDTO> podMetrics = List.of(
                createSeries("qps", List.of(
                        createPoint(Instant.parse("2025-01-23T09:00:00Z"), 10.0),
                        createPoint(Instant.parse("2025-01-23T09:01:00Z"), 20.0)
                )),
                createSeries("qps", List.of(
                        createPoint(Instant.parse("2025-01-23T09:00:00Z"), 15.0),
                        createPoint(Instant.parse("2025-01-23T09:01:00Z"), 25.0)
                ))
        );

        MetricSeriesDTO result = aggregator.aggregate(podMetrics, AggregationType.SUM);

        assertEquals("sum", result.getAggregation());
        assertEquals(2, result.getDataPoints().size());
        assertEquals(25.0, result.getDataPoints().get(0).getValue(), 0.01);
        assertEquals(45.0, result.getDataPoints().get(1).getValue(), 0.01);
    }

    @Test
    void aggregateWeightedAvg() {
        List<MetricSeriesDTO> podMetrics = List.of(
                createSeries("success_rate", List.of(
                        createPoint(Instant.parse("2025-01-23T09:00:00Z"), 95.0),
                        createPoint(Instant.parse("2025-01-23T09:01:00Z"), 98.0)
                )),
                createSeries("success_rate", List.of(
                        createPoint(Instant.parse("2025-01-23T09:00:00Z"), 90.0),
                        createPoint(Instant.parse("2025-01-23T09:01:00Z"), 96.0)
                ))
        );

        MetricSeriesDTO result = aggregator.aggregate(podMetrics, AggregationType.WEIGHTED_AVG);

        assertEquals("weighted_avg", result.getAggregation());
        assertEquals(2, result.getDataPoints().size());
        assertEquals(92.5, result.getDataPoints().get(0).getValue(), 0.01);
        assertEquals(97.0, result.getDataPoints().get(1).getValue(), 0.01);
    }

    private MetricSeriesDTO createSeries(String name, List<DataPointDTO> points) {
        return MetricSeriesDTO.builder()
                .metricName(name)
                .dataPoints(points)
                .build();
    }

    private DataPointDTO createPoint(Instant timestamp, double value) {
        return DataPointDTO.builder()
                .timestamp(timestamp)
                .value(value)
                .build();
    }
}
