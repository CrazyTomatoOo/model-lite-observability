package com.modelengine.observability.client;

import com.modelengine.observability.client.dto.PrometheusData;
import com.modelengine.observability.client.dto.PrometheusResponse;
import com.modelengine.observability.client.dto.PrometheusResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Primary
public class MockPrometheusClient implements PrometheusClient {

    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("model_name=\"([^\"]+)\"");

    private static final Map<String, ModelProfile> PROFILES = new HashMap<>();

    static {
        PROFILES.put("llama3-70b", new ModelProfile(
                500.0, 30.0, 500.0, 1200.0, 800.0, 10.0, 3.0, 13.0, 128.0, 0.99,
                10000L, 100L, 9900L
        ));
        PROFILES.put("qwen2-7b", new ModelProfile(
                250.0, 18.0, 300.0, 900.0, 600.0, 8.0, 2.0, 10.0, 64.0, 0.98,
                8000L, 160L, 7840L
        ));
        PROFILES.put("glm4-9b", new ModelProfile(
                150.0, 30.0, 800.0, 2500.0, 120.0, 2.5, 0.5, 3.0, 12.0, 0.995,
                1500L, 3L, 1497L
        ));
    }

    @Override
    public PrometheusResponse query(String promQL) {
        String modelName = extractModelName(promQL);
        String metricType = detectMetricType(promQL);
        log.debug("Mock instant query: metric={}, model={}, promQL={}", metricType, modelName, promQL);

        ModelProfile profile = PROFILES.getOrDefault(modelName, PROFILES.get("llama3-70b"));
        double value = getMetricValue(profile, metricType, false);

        long timestampSeconds = System.currentTimeMillis() / 1000;

        PrometheusResult result = PrometheusResult.builder()
                .metric(Map.of("model_name", modelName, "__name__", metricType))
                .value(List.of(timestampSeconds, value))
                .build();

        return PrometheusResponse.builder()
                .status("success")
                .data(PrometheusData.builder()
                        .resultType("vector")
                        .result(List.of(result))
                        .build())
                .build();
    }

    @Override
    public PrometheusResponse queryRange(String promQL, Instant start, Instant end, Duration step, Duration timeout) {
        String modelName = extractModelName(promQL);
        String metricType = detectMetricType(promQL);
        // Use current time for mock data end time
        Instant now = Instant.now();
        Instant effectiveEnd = end.isAfter(now) ? now : end;
        log.debug("Mock range query: metric={}, model={}, start={}, end={}, step={}",
                metricType, modelName, start, effectiveEnd, step);

        ModelProfile profile = PROFILES.getOrDefault(modelName, PROFILES.get("llama3-70b"));
        double baseValue = getMetricValue(profile, metricType, true);

        List<List<Object>> values = generateTimeSeries(start, effectiveEnd, step, baseValue, metricType);
        PrometheusResult result = PrometheusResult.builder()
                .metric(Map.of("model_name", modelName, "__name__", metricType))
                .values(values)
                .build();

        return PrometheusResponse.builder()
                .status("success")
                .data(PrometheusData.builder()
                        .resultType("matrix")
                        .result(List.of(result))
                        .build())
                .build();
    }

    @Override
    public boolean ping() {
        log.debug("Mock Prometheus ping: UP");
        return true;
    }

    private String extractModelName(String promQL) {
        Matcher m = MODEL_NAME_PATTERN.matcher(promQL);
        if (m.find()) {
            return m.group(1);
        }
        log.warn("Could not extract model_name from PromQL, defaulting to llama3-70b: {}", promQL);
        return "llama3-70b";
    }

    private String detectMetricType(String promQL) {
        if (promQL.contains("time_to_first_token")) return "ttft";
        if (promQL.contains("time_per_output_token")) return "tpot";
        if (promQL.contains("generation_tokens")) return "generation_tokens";
        if (promQL.contains("prompt_throughput")) return "prompt_throughput";
        if (promQL.contains("generation_throughput") || promQL.contains("decode_throughput")) return "decode_throughput";
        if (promQL.contains("rate(") && promQL.contains("request_received")) return "qps";
        if (promQL.contains("num_requests_running") && !promQL.contains("+")) return "connections";
        if (promQL.contains("num_requests_waiting") && !promQL.contains("+")) return "waiting_connections";
        if (promQL.contains("num_requests_running") && promQL.contains("num_requests_waiting")) return "total_connections";
        if (promQL.contains("request_success") && promQL.contains("request_received") && promQL.contains("/")) return "success_rate";
        if (promQL.contains("request_failed")) return "failed_requests";
        if (promQL.contains("request_success")) return "success_requests";
        if (promQL.contains("request_received")) return "total_requests";
        log.debug("Unknown metric type in PromQL: {}", promQL);
        return "unknown";
    }

    private double getMetricValue(ModelProfile profile, String metricType, boolean isRange) {
        double base = switch (metricType) {
            case "ttft"              -> profile.ttftMs;
            case "tpot"              -> profile.tpotMs;
            case "generation_tokens"  -> profile.generationTokens;
            case "prompt_throughput"  -> profile.promptThroughput;
            case "decode_throughput"  -> profile.decodeThroughput;
            case "qps"               -> profile.qps;
            case "connections"        -> profile.connections;
            case "waiting_connections" -> profile.waitingConnections;
            case "total_connections"  -> profile.totalConnections;
            case "success_rate"       -> profile.successRate * 100;
            case "total_requests"     -> (double) profile.totalRequests;
            case "failed_requests"    -> (double) profile.failedRequests;
            case "success_requests"   -> (double) profile.successRequests;
            default -> {
                log.debug("Unknown metric type '{}', returning 0", metricType);
                yield 0.0;
            }
        };

        if (!isRange) {
            double jitterFactor = 0.95 + ThreadLocalRandom.current().nextDouble() * 0.10;
            return Math.round(base * jitterFactor * 100.0) / 100.0;
        }
        return base;
    }

    private List<List<Object>> generateTimeSeries(Instant start, Instant end, Duration step,
                                                   double baseValue, String metricType) {
        List<List<Object>> values = new ArrayList<>();
        long startEpoch = start.getEpochSecond();
        long endEpoch = end.getEpochSecond();
        long stepSeconds = step.getSeconds();

        if (stepSeconds <= 0) {
            stepSeconds = 60;
        }

        long totalSpan = endEpoch - startEpoch;
        if (totalSpan <= 0) {
            totalSpan = 600;
        }

        double jitterRatio = getJitterRatio(metricType);

        for (long t = startEpoch; t <= endEpoch; t += stepSeconds) {
            double progress = (double)(t - startEpoch) / totalSpan;
            double sine = Math.sin(progress * 2.0 * Math.PI * 3.0);
            double sineComponent = baseValue * 0.08 * sine;

            double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * baseValue * jitterRatio;

            double value = baseValue + sineComponent + noise;

            if (metricType.equals("success_rate")) {
                value = Math.min(100.0, Math.max(0.0, value));
            } else {
                value = Math.max(0.0, value);
            }

            value = Math.round(value * 100.0) / 100.0;
            values.add(List.of(t, value));
        }

        return values;
    }

    private double getJitterRatio(String metricType) {
        return switch (metricType) {
            case "ttft", "tpot"              -> 0.12;
            case "prompt_throughput", "decode_throughput", "qps" -> 0.10;
            case "connections", "waiting_connections", "total_connections" -> 0.05;
            case "generation_tokens"         -> 0.08;
            case "success_rate"              -> 0.02;
            case "total_requests", "failed_requests", "success_requests" -> 0.05;
            default -> 0.08;
        };
    }

    private record ModelProfile(
            double ttftMs,
            double tpotMs,
            double generationTokens,
            double promptThroughput,
            double decodeThroughput,
            double qps,
            double connections,
            double waitingConnections,
            double totalConnections,
            double successRate,
            long totalRequests,
            long failedRequests,
            long successRequests
    ) {}
}
