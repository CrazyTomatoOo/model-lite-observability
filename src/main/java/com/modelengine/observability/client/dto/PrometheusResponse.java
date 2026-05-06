package com.modelengine.observability.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a Prometheus API response.
 * 
 * Response format:
 * {
 *   "status": "success",
 *   "data": {
 *     "resultType": "vector",
 *     "result": [...]
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusResponse {
    
    /**
     * Status of the query: "success" or "error"
     */
    private String status;
    
    /**
     * Error type (only present when status is "error")
     */
    private String errorType;
    
    /**
     * Error message (only present when status is "error")
     */
    private String error;
    
    /**
     * Data payload containing query results
     */
    private PrometheusData data;
    
    /**
     * Checks if the query was successful
     */
    public boolean isSuccess() {
        return "success".equals(status);
    }
    
    /**
     * Checks if the response has data
     */
    public boolean hasData() {
        return data != null && data.getResult() != null && !data.getResult().isEmpty();
    }
}
