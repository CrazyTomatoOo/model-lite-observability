package com.modelengine.observability.service;

import com.modelengine.observability.cache.CacheManager;
import com.modelengine.observability.client.PrometheusClient;
import com.modelengine.observability.client.dto.PrometheusData;
import com.modelengine.observability.client.dto.PrometheusResponse;
import com.modelengine.observability.client.dto.PrometheusResult;
import com.modelengine.observability.config.MetricsDefinitionLoader;
import com.modelengine.observability.config.ObservabilityProperties;
import com.modelengine.observability.service.MetricsAggregator.AggregationType;
import com.modelengine.observability.dto.DataPointDTO;
import com.modelengine.observability.dto.MetricSeriesDTO;
import com.modelengine.observability.dto.MetricsRangeQueryDTO;
import com.modelengine.observability.dto.MetricsRangeResponseDTO;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private PrometheusClient prometheusClient;

    @Mock
    private TopologyService topologyService;

    @Mock
    private MetricsAggregator metricsAggregator;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ObservabilityProperties properties;

    @Mock
    private MetricsDefinitionLoader metricsDefinitionLoader;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getOrLoad(anyString(), any(), any())).thenAnswer(invocation -> {
            var loader = invocation.getArgument(1, java.util.function.Supplier.class);
            return loader.get();
        });
        lenient().when(properties.getCache()).thenReturn(new ObservabilityProperties.Cache());

        metricsService = new MetricsService(
                prometheusClient, topologyService, metricsAggregator,
                cacheManager, properties, metricsDefinitionLoader);
    }

    // ─── Helpers ───

    private Pod createPod(String name) {
        Pod pod = new Pod();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        pod.setMetadata(metadata);
        return pod;
    }

    private MetricsDefinitionLoader.MetricDefinition createDef(String name, String aggregation) {
        AggregationType agg = AggregationType.valueOf(aggregation.toUpperCase());
        return new MetricsDefinitionLoader.MetricDefinition(name, name + "{%s}", name + " Display", "count", agg);
    }

    private PrometheusResponse createSuccessResponse(List<List<Object>> values) {
        PrometheusResult result = new PrometheusResult();
        result.setValues(values);

        PrometheusData data = new PrometheusData();
        data.setResultType("matrix");
        data.setResult(List.of(result));

        return PrometheusResponse.builder()
                .status("success")
                .data(data)
                .build();
    }

    private MetricSeriesDTO createSeries(String name, List<DataPointDTO> points) {
        return MetricSeriesDTO.builder()
                .metricName(name)
                .displayName(name + " Display")
                .unit("count")
                .aggregation("avg")
                .dataPoints(points)
                .build();
    }

    // ─── Tests ───

    @Test
    void responseContainsInstanceNameNotServiceId() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("my-instance")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def = createDef("ttft", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "100.0"),
                List.of(1737622860L, "200.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("ttft", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(100.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622860)).value(200.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), eq(MetricsAggregator.AggregationType.AVG)))
                .thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("my-instance", query);

        // Then
        assertNotNull(response);
        assertEquals("my-instance", response.getInstanceName());
        assertNotNull(response.getStartTime());
        assertNotNull(response.getEndTime());
        assertNotNull(response.getMetrics());
    }

    @Test
    void emptyMetricsQueriesAllDefinitions() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-1")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def1 = createDef("ttft", "AVG");
        MetricsDefinitionLoader.MetricDefinition def2 = createDef("qps", "SUM");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def1, def2));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "100.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("test", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(100.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), any()))
                .thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .metrics(List.of())  // empty → all
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-1", query);

        // Then
        assertNotNull(response);
        // Both defs should be queried
        verify(metricsDefinitionLoader, times(1)).getDefinitions();
        assertEquals(2, response.getMetrics().size());
    }

    @Test
    void nullMetricsQueriesAllDefinitions() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-2")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def = createDef("ttft", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "100.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("ttft", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(100.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), any())).thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .metrics(null)  // null → all
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-2", query);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getMetrics().size());
        assertEquals("ttft", response.getMetrics().get(0).getMetricName());
    }

    @Test
    void specificMetricsFilterQuery() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-3")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def1 = createDef("ttft", "AVG");
        MetricsDefinitionLoader.MetricDefinition def2 = createDef("qps", "SUM");
        MetricsDefinitionLoader.MetricDefinition def3 = createDef("tpot", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def1, def2, def3));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "100.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("test", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(100.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), any())).thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .metrics(List.of("ttft", "qps"))  // only these two
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-3", query);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getMetrics().size());  // only ttft and qps
    }

    @Test
    void limitTruncatesDataPoints() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-4")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def = createDef("ttft", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "1.0"),
                List.of(1737622860L, "2.0"),
                List.of(1737622920L, "3.0"),
                List.of(1737622980L, "4.0"),
                List.of(1737623040L, "5.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        // aggregator should return the series of 5 points
        MetricSeriesDTO aggregated = createSeries("ttft", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(1.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622860)).value(2.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622920)).value(3.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622980)).value(4.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737623040)).value(5.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), any())).thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .limit(3)
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-4", query);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getMetrics().size());
        assertEquals(3, response.getMetrics().get(0).getDataPoints().size());  // truncated to 3
        assertEquals(1.0, response.getMetrics().get(0).getDataPoints().get(0).getValue());
        assertEquals(3.0, response.getMetrics().get(0).getDataPoints().get(2).getValue());
    }

    @Test
    void limitZeroMeansNoTruncation() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-5")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def = createDef("ttft", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "1.0"),
                List.of(1737622860L, "2.0"),
                List.of(1737622920L, "3.0"),
                List.of(1737622980L, "4.0"),
                List.of(1737623040L, "5.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("ttft", new ArrayList<>(List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(1.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622860)).value(2.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622920)).value(3.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622980)).value(4.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737623040)).value(5.0).build()
        )));
        when(metricsAggregator.aggregate(anyList(), any())).thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .limit(0)  // 0 = no limit
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-5", query);

        // Then
        assertEquals(5, response.getMetrics().get(0).getDataPoints().size());  // not truncated
    }

    @Test
    void limitNullMeansNoTruncation() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-6")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def = createDef("ttft", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "1.0"),
                List.of(1737622860L, "2.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("ttft", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(1.0).build(),
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622860)).value(2.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), any())).thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .limit(null)
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-6", query);

        // Then
        assertEquals(2, response.getMetrics().get(0).getDataPoints().size());
    }

    @Test
    void unixTimestampStartAndEndTime() {
        // Given
        Pod pod = createPod("test-pod");
        when(topologyService.getPodsForInstance("inst-7")).thenReturn(List.of(pod));

        MetricsDefinitionLoader.MetricDefinition def = createDef("ttft", "AVG");
        when(metricsDefinitionLoader.getDefinitions()).thenReturn(List.of(def));

        PrometheusResponse promResp = createSuccessResponse(List.of(
                List.of(1737622800L, "100.0")
        ));
        when(prometheusClient.queryRange(anyString(), any(), any(), any(), any())).thenReturn(promResp);

        MetricSeriesDTO aggregated = createSeries("ttft", List.of(
                DataPointDTO.builder().timestamp(Instant.ofEpochSecond(1737622800)).value(100.0).build()
        ));
        when(metricsAggregator.aggregate(anyList(), any())).thenReturn(aggregated);

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime(1737622800)  // Unix timestamp number
                .endTime(1737626400)    // Unix timestamp number
                .step(60)               // number = seconds
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("inst-7", query);

        // Then
        assertNotNull(response);
        assertEquals("inst-7", response.getInstanceName());
        verify(prometheusClient).queryRange(
                anyString(),
                eq(Instant.ofEpochSecond(1737622800)),
                eq(Instant.ofEpochSecond(1737626400)),
                eq(Duration.ofSeconds(60)),
                any()
        );
    }

    @Test
    void noPodsReturnsEmptyResponse() {
        // Given
        when(topologyService.getPodsForInstance("empty-inst")).thenReturn(List.of());

        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .step("60s")
                .build();

        // When
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange("empty-inst", query);

        // Then
        assertNotNull(response);
        assertEquals("empty-inst", response.getInstanceName());
        assertTrue(response.getMetrics().isEmpty());
        verify(prometheusClient, never()).queryRange(anyString(), any(), any(), any(), any());
    }

    @Test
    void defaultTimeoutAndLimitInBuilder() {
        MetricsRangeQueryDTO query = MetricsRangeQueryDTO.builder()
                .startTime("2025-01-23T00:00:00Z")
                .endTime("2025-01-23T01:00:00Z")
                .build();

        assertEquals("30s", query.getTimeout());
        assertEquals(0, query.getLimit());
    }
}
