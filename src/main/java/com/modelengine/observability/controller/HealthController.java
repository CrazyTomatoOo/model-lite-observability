package com.modelengine.observability.controller;

import com.modelengine.observability.dto.ApiResponse;
import com.modelengine.observability.dto.HealthStatusDTO;
import com.modelengine.observability.exception.ErrorCode;
import com.modelengine.observability.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check APIs")
public class HealthController {

    private final HealthCheckService healthCheckService;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthStatusDTO>> health() {
        HealthStatusDTO status = healthCheckService.checkHealth();

        if ("healthy".equals(status.getStatus())) {
            return ResponseEntity.ok(ApiResponse.success(status));
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR,
                        "Service is unhealthy: one or more components are disconnected"));
    }
}
