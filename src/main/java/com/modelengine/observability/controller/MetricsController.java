package com.modelengine.observability.controller;

import com.modelengine.observability.dto.ApiResponse;
import com.modelengine.observability.dto.MetricsRangeQueryDTO;
import com.modelengine.observability.dto.MetricsRangeResponseDTO;
import com.modelengine.observability.service.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/model-services")
@RequiredArgsConstructor
public class MetricsController {
    private final MetricsService metricsService;

    @PostMapping("/{instanceName}/metrics/range")
    public ResponseEntity<ApiResponse<MetricsRangeResponseDTO>> getMetricsRange(
            @PathVariable String instanceName,
            @Valid @RequestBody MetricsRangeQueryDTO query) {
        try {
            MetricsRangeResponseDTO r = metricsService.getServiceMetricsRange(instanceName, query);
            return ResponseEntity.ok(ApiResponse.success(r));
        } catch (Throwable t) {
            System.out.println("METRICS_FATAL: " + t.getClass().getName() + " -> " + t.getMessage());
            t.printStackTrace(System.out);
            throw new RuntimeException(t);
        }
    }
}
