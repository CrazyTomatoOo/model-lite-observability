package com.modelengine.observability.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelengine.observability.client.dto.PrometheusData;
import com.modelengine.observability.client.dto.PrometheusResponse;
import com.modelengine.observability.client.dto.PrometheusResult;
import com.modelengine.observability.config.ObservabilityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link PrometheusClient} using RestTemplate.
 * Supports retries with exponential backoff and connection error handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusClientImpl implements PrometheusClient {
    
    private final ObservabilityProperties properties;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;
    
    @PostConstruct
    void init() {
        int timeoutMillis = (int) properties.getPrometheus().getTimeout().toMillis();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        this.restTemplate = new RestTemplate(factory);
    }
    
    @Override
    public PrometheusResponse query(String promQL) {
        log.debug("Executing Prometheus instant query: {}", promQL);
        URI uri = UriComponentsBuilder
            .fromHttpUrl(properties.getPrometheus().getUrl())
            .path("/api/v1/query")
            .queryParam("query", promQL)
            .encode()
            .build()
            .toUri();
        return executeWithRetry(uri, "instant query");
    }
    
    @Override
    public PrometheusResponse queryRange(String promQL, Instant start, Instant end, Duration step, Duration timeout) {
        log.debug("Executing Prometheus range query: {} from {} to {} step {}", 
                  promQL, start, end, step);
        URI uri = UriComponentsBuilder
            .fromHttpUrl(properties.getPrometheus().getUrl())
            .path("/api/v1/query_range")
            .queryParam("query", promQL)
            .queryParam("start", start.getEpochSecond())
            .queryParam("end", end.getEpochSecond())
            .queryParam("step", step.getSeconds())
            .queryParam("timeout", timeout.toSeconds() + "s")
            .encode()
            .build()
            .toUri();
        return executeWithRetry(uri, "range query");
    }
    
    @Override
    public boolean ping() {
        try {
            URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.getPrometheus().getUrl())
                .path("/-/healthy")
                .build()
                .toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            boolean healthy = response.getStatusCode().is2xxSuccessful();
            log.debug("Prometheus health check: {}", healthy ? "UP" : "DOWN");
            return healthy;
        } catch (Exception e) {
            log.warn("Prometheus health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private PrometheusResponse executeWithRetry(URI uri, String operation) {
        int maxRetries = properties.getPrometheus().getMaxRetries();
        Duration backoff = properties.getPrometheus().getRetryBackoff();
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
                return parseResponse(response.getBody());
            } catch (ResourceAccessException e) {
                log.warn("Prometheus {} connection error (attempt {}/{}): {}", 
                         operation, attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    sleep(backoff.multipliedBy(attempt));
                }
            } catch (RestClientResponseException e) {
                log.error("Prometheus {} HTTP error {}: {}", 
                          operation, e.getStatusCode(), e.getResponseBodyAsString());
                return errorResponse("http_error", 
                    "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
            } catch (Exception e) {
                log.error("Prometheus {} unexpected error: {}", operation, e.getMessage());
                return errorResponse("unexpected_error", e.getMessage());
            }
        }
        
        log.error("Prometheus {} failed after {} retries", operation, maxRetries);
        return errorResponse("connection_failed", 
            "Failed to connect to Prometheus after " + maxRetries + " retries");
    }
    
    private PrometheusResponse parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            PrometheusResponse.PrometheusResponseBuilder response = PrometheusResponse.builder();
            
            response.status(root.path("status").asText());
            response.errorType(root.path("errorType").asText(null));
            response.error(root.path("error").asText(null));
            
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode()) {
                PrometheusData data = new PrometheusData();
                data.setResultType(dataNode.path("resultType").asText());
                data.setResult(parseResults(dataNode.path("result")));
                response.data(data);
            }
            
            return response.build();
        } catch (Exception e) {
            log.error("Failed to parse Prometheus response: {}", e.getMessage());
            return errorResponse("parse_error", "Failed to parse response: " + e.getMessage());
        }
    }
    
    private List<PrometheusResult> parseResults(JsonNode resultsNode) {
        List<PrometheusResult> results = new ArrayList<>();
        if (resultsNode.isMissingNode() || !resultsNode.isArray()) {
            return results;
        }
        
        for (JsonNode resultNode : resultsNode) {
            PrometheusResult result = new PrometheusResult();
            
            JsonNode metricNode = resultNode.path("metric");
            if (!metricNode.isMissingNode()) {
                Map<String, String> metric = objectMapper.convertValue(metricNode, Map.class);
                result.setMetric(metric);
            }
            
            if (resultNode.has("value")) {
                result.setValue(resultNode.path("value"));
            }
            if (resultNode.has("values")) {
                result.setValues(resultNode.path("values"));
            }
            
            results.add(result);
        }
        return results;
    }
    
    private PrometheusResponse errorResponse(String errorType, String error) {
        return PrometheusResponse.builder()
            .status("error")
            .errorType(errorType)
            .error(error)
            .build();
    }
    
    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
