package com.modelengine.observability.client;

import com.modelengine.observability.client.dto.PrometheusResponse;

import java.time.Duration;
import java.time.Instant;

/**
 * Client for querying Prometheus metrics API.
 */
public interface PrometheusClient {
    
    /**
     * Execute an instant PromQL query.
     *
     * @param promQL PromQL query string
     * @return parsed Prometheus response
     */
    PrometheusResponse query(String promQL);
    
    /**
     * Execute a range PromQL query over a time range.
     *
     * @param promQL PromQL query string
     * @param start  start time
     * @param end    end time
     * @param step   query resolution step width
     * @return parsed Prometheus response
     */
    PrometheusResponse queryRange(String promQL, Instant start, Instant end, Duration step, Duration timeout);
    
    /**
     * Check connectivity to Prometheus.
     *
     * @return true if Prometheus is reachable
     */
    boolean ping();
}
