package com.modelengine.observability.controller;

import com.modelengine.observability.dto.ApiResponse;
import com.modelengine.observability.dto.MetricsRangeQueryDTO;
import com.modelengine.observability.dto.MetricsRangeResponseDTO;
import com.modelengine.observability.service.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/model-services")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Model service metrics APIs")
public class MetricsController {
    private final MetricsService metricsService;

    @Operation(summary = "Get metrics range", description = "Query time-series metrics for a model service instance")
    @PostMapping("/{instanceName}/metrics/range")
    public ResponseEntity<ApiResponse<MetricsRangeResponseDTO>> getMetricsRange(
            @PathVariable String instanceName,
            @Valid @RequestBody MetricsRangeQueryDTO query) {
        try {
            MetricsRangeResponseDTO r = metricsService.getServiceMetricsRange(instanceName, query);
            return ResponseEntity.ok(ApiResponse.success(r));
        } catch (Throwable t) {
            log.error("Error fetching metrics for instance: {}", instanceName, t);
            throw new RuntimeException(t);
        }
    }
}
