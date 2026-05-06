package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO for metrics range query request.
 * Matches the MetricsRangeQueryRequest schema from the OpenAPI spec.
 * <p>
 * startTime and endTime accept either RFC 3339 strings or Unix epoch-second numbers.
 * step accepts either Duration strings (e.g. "60s") or numeric seconds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsRangeQueryDTO {

    @Valid
    @ValidMetrics
    private List<String> metrics;

    @NotNull
    private Object startTime;

    @NotNull
    private Object endTime;

    private Object step;

    @Builder.Default
    private String timeout = "30s";

    @Builder.Default
    private Integer limit = 0;
}
