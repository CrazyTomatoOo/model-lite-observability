package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for metrics range query response.
 * Matches the MetricsRangeResponse schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsRangeResponseDTO {

    private String instanceName;
    private Instant startTime;
    private Instant endTime;
    private String step;
    private List<MetricSeriesDTO> metrics;
}
