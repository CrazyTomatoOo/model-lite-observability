package com.modelengine.observability.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing a single result entry in a Prometheus API response.
 * 
 * Format:
 * {
 *   "metric": {"__name__": "up", "job": "prometheus"},
 *   "value": [1234567890, "1"]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusResult {
    
    /**
     * Metric labels including __name__
     */
    private Map<String, String> metric;
    
    /**
     * For instant queries: single value [timestamp, value]
     */
    private Object value;
    
    /**
     * For range queries: list of values [[timestamp, value], ...]
     */
    private Object values;
    
    /**
     * Gets the metric name from the labels
     */
    public String getMetricName() {
        return metric != null ? metric.get("__name__") : null;
    }
}
