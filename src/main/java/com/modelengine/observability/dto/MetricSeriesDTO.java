package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a metric series.
 * Matches the MetricSeries schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSeriesDTO {

    private String metricName;
    private String displayName;
    private String unit;
    private String aggregation;
    private List<DataPointDTO> dataPoints;
}
