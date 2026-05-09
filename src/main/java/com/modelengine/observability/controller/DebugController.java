package com.modelengine.observability.controller;

import com.modelengine.observability.config.MetricsDefinitionLoader;
import com.modelengine.observability.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Debug and definition APIs")
public class DebugController {

    private final MetricsDefinitionLoader loader;

    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> definitions() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("count", loader.getDefinitions().size());
        info.put("names", loader.getDefinitions().stream()
                .map(MetricsDefinitionLoader.MetricDefinition::metricName)
                .toList());
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}
