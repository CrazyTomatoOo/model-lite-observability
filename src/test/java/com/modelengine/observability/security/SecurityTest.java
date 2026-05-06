package com.modelengine.observability.security;

import com.modelengine.observability.dto.HealthStatusDTO;
import com.modelengine.observability.service.HealthCheckService;
import com.modelengine.observability.service.ModelServiceService;
import com.modelengine.observability.service.MetricsService;
import com.modelengine.observability.config.SecurityConfig;
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

/**
 * Tests for JWT security skeleton:
 * - /health is public (no token required)
 * - Other endpoints require Bearer token
 * - 401 returns proper error JSON with errorCode and errorType
 */
@WebMvcTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthCheckService healthCheckService;

    @MockBean
    private ModelServiceService modelServiceService;

    @MockBean
    private MetricsService metricsService;
    private HealthStatusDTO buildHealthyStatus() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "connected");
        components.put("kubernetes", "connected");
        return HealthStatusDTO.builder()
                .status("healthy")
                .version("2.0.0")
                .timestamp(Instant.now())
                .clusterId("test-cluster")
                .meVersion("2.0.0")
                .components(components)
                .build();
    }

    @Test
    void healthEndpoint_noToken_returns200() throws Exception {
        when(healthCheckService.checkHealth()).thenReturn(buildHealthyStatus());

        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("healthy"));
    }

    @Test
    void modelServices_noToken_returns401() throws Exception {
        mockMvc.perform(get("/model-services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorCode").value("0108002"))
                .andExpect(jsonPath("$.errorType").value("Unauthorized"));
    }

    @Test
    void modelServices_withBearerToken_returns200() throws Exception {
        mockMvc.perform(get("/model-services")
                        .header("Authorization", "Bearer any-token-value")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void unauthorizedResponse_containsCorrectErrorCodeAndErrorType() throws Exception {
        mockMvc.perform(get("/model-services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorCode").value("0108002"))
                .andExpect(jsonPath("$.errorType").value("Unauthorized"))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void modelServices_withMalformedAuthHeader_returns401() throws Exception {
        mockMvc.perform(get("/model-services")
                        .header("Authorization", "NotBearer sometoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modelServices_withEmptyBearer_returns401() throws Exception {
        mockMvc.perform(get("/model-services")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
