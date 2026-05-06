package com.modelengine.observability.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthStatusDTOTest {

    @Test
    void builderCreatesDtoWithFlatComponents() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "connected");
        components.put("kubernetes", "disconnected");

        HealthStatusDTO dto = HealthStatusDTO.builder()
                .status("healthy")
                .version("2.0.0")
                .timestamp(Instant.now())
                .clusterId("cluster-01")
                .meVersion("2.0.0")
                .components(components)
                .build();

        assertEquals("healthy", dto.getStatus());
        assertEquals("2.0.0", dto.getVersion());
        assertEquals("cluster-01", dto.getClusterId());
        assertEquals("2.0.0", dto.getMeVersion());
        assertNotNull(dto.getTimestamp());
        assertEquals("connected", dto.getComponents().get("prometheus"));
        assertEquals("disconnected", dto.getComponents().get("kubernetes"));
    }

    @Test
    void componentsValuesAreFlatStrings() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "connected");
        components.put("kubernetes", "connected");

        HealthStatusDTO dto = HealthStatusDTO.builder()
                .status("healthy")
                .components(components)
                .build();

        for (Map.Entry<String, String> entry : dto.getComponents().entrySet()) {
            assertInstanceOf(String.class, entry.getValue(),
                    "Component value for '" + entry.getKey() + "' must be a flat String, not a nested object");
        }
    }

    @Test
    void unhealthyStatus() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("prometheus", "disconnected");
        components.put("kubernetes", "connected");

        HealthStatusDTO dto = HealthStatusDTO.builder()
                .status("unhealthy")
                .components(components)
                .build();

        assertEquals("unhealthy", dto.getStatus());
        assertEquals("disconnected", dto.getComponents().get("prometheus"));
    }

    @Test
    void clusterIdAndMeVersionAreOptional() {
        HealthStatusDTO dto = HealthStatusDTO.builder()
                .status("healthy")
                .build();

        assertNull(dto.getClusterId());
        assertNull(dto.getMeVersion());
    }
}
