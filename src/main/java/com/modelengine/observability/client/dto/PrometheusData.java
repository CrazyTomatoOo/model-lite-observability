package com.modelengine.observability.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the data section of a Prometheus API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusData {
    
    /**
     * Type of result: "vector", "matrix", "scalar", or "string"
     */
    private String resultType;
    
    /**
     * List of result entries
     */
    private List<PrometheusResult> result;
}
