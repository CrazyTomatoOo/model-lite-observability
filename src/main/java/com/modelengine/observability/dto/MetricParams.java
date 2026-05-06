package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing performance metrics.
 * Matches the MetricParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricParams {

    private Integer requests;
    private Integer responses;
    private Integer exceptions;
    private Double speed;
    private Integer totalInputTokens;
    private Integer totalOutputTokens;
    private Double latency;
}
