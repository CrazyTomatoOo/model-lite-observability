package com.modelengine.observability.dto;

/**
 * Enumeration of supported metric names for the Observability API.
 * These 13 metrics cover TTFT, TPOT, token throughput, QPS, connections,
 * waiting connections, total connections, success rate, and request counts.
 */
public enum MetricName {

    TTFT("ttft"),
    TPOT("tpot"),
    GENERATION_TOKENS("generation_tokens"),
    PROMPT_THROUGHPUT("prompt_throughput"),
    DECODE_THROUGHPUT("decode_throughput"),
    QPS("qps"),
    CONNECTIONS("connections"),
    WAITING_CONNECTIONS("waiting_connections"),
    TOTAL_CONNECTIONS("total_connections"),
    SUCCESS_RATE("success_rate"),
    TOTAL_REQUESTS("total_requests"),
    FAILED_REQUESTS("failed_requests"),
    SUCCESS_REQUESTS("success_requests");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MetricName fromValue(String value) {
        for (MetricName m : values()) {
            if (m.value.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown metric: " + value);
    }

    public static boolean isValid(String value) {
        for (MetricName m : values()) {
            if (m.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
