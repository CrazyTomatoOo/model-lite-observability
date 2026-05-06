package com.modelengine.observability.controller;

import com.modelengine.observability.dto.ApiResponse;
import com.modelengine.observability.dto.MetricsRangeQueryDTO;
import com.modelengine.observability.dto.MetricsRangeResponseDTO;
import com.modelengine.observability.service.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/model-services")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @PostMapping("/{instanceName}/metrics/range")
    public ResponseEntity<ApiResponse<MetricsRangeResponseDTO>> getMetricsRange(
            @PathVariable String instanceName,
            @Valid @RequestBody MetricsRangeQueryDTO query) {
        MetricsRangeResponseDTO response = metricsService.getServiceMetricsRange(instanceName, query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
