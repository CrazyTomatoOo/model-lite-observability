package com.modelengine.observability.controller;

import com.modelengine.observability.dto.HealthStatusDTO;
import com.modelengine.observability.service.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthCheckService healthCheckService;

    @Test
    void healthyReturns200() throws Exception {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "connected");
        components.put("kubernetes", "connected");

        HealthStatusDTO status = HealthStatusDTO.builder()
                .status("healthy")
                .version("2.0.0")
                .timestamp(Instant.now())
                .clusterId("test-cluster")
                .meVersion("2.0.0")
                .components(components)
                .build();

        when(healthCheckService.checkHealth()).thenReturn(status);

        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("healthy"))
                .andExpect(jsonPath("$.data.clusterId").value("test-cluster"))
                .andExpect(jsonPath("$.data.meVersion").value("2.0.0"))
                .andExpect(jsonPath("$.data.components.prometheus").value("connected"))
                .andExpect(jsonPath("$.data.components.kubernetes").value("connected"));
    }

    @Test
    void unhealthyReturns503() throws Exception {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "disconnected");
        components.put("kubernetes", "connected");

        HealthStatusDTO status = HealthStatusDTO.builder()
                .status("unhealthy")
                .version("2.0.0")
                .timestamp(Instant.now())
                .clusterId("test-cluster")
                .meVersion("2.0.0")
                .components(components)
                .build();

        when(healthCheckService.checkHealth()).thenReturn(status);

        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorCode").value("0108007"))
                .andExpect(jsonPath("$.errorType").value("InternalError"))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void componentsAreFlatStrings() throws Exception {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "connected");
        components.put("kubernetes", "connected");

        HealthStatusDTO status = HealthStatusDTO.builder()
                .status("healthy")
                .version("2.0.0")
                .timestamp(Instant.now())
                .clusterId("test-cluster")
                .meVersion("2.0.0")
                .components(components)
                .build();

        when(healthCheckService.checkHealth()).thenReturn(status);

        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.components.prometheus").isString())
                .andExpect(jsonPath("$.data.components.kubernetes").isString());
    }


    @Test
    void responseContainsAllRequiredFields() throws Exception {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "connected");
        components.put("kubernetes", "connected");

        HealthStatusDTO status = HealthStatusDTO.builder()
                .status("healthy")
                .version("2.0.0")
                .timestamp(Instant.now())
                .clusterId("prod-01")
                .meVersion("3.0.0")
                .components(components)
                .build();

        when(healthCheckService.checkHealth()).thenReturn(status);

        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.status").exists())
                .andExpect(jsonPath("$.data.version").exists())
                .andExpect(jsonPath("$.data.timestamp").exists())
                .andExpect(jsonPath("$.data.clusterId").exists())
                .andExpect(jsonPath("$.data.meVersion").exists())
                .andExpect(jsonPath("$.data.components").exists());
    }
}
