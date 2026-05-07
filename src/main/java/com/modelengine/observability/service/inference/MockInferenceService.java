package com.modelengine.observability.service.inference;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mock implementation of InferenceService returning hardcoded MindIE instances.
 * Exporter metric labels (model_name) are kept consistent with instance names here.
 *
 * <p>Two models: llama3-70b (heavy), qwen2-7b (light).</p>
 */
@Slf4j
@Service
@Primary
public class MockInferenceService implements InferenceService {

    static final List<InferenceInstance> INSTANCES = List.of(
        InferenceInstance.builder()
            .instanceName("llama3-70b")
            .namespace("model-engine")
            .status("Running")
            .currentReplicas(4)
            .desiredReplicas(4)
            .framework("MindIE")
            .address("10.1.0.10:8080")
            .selector(Map.of("model_name", "llama3-70b"))
            .labels(Map.of("model_name", "llama3-70b", "framework", "MindIE"))
            .build(),
        InferenceInstance.builder()
            .instanceName("qwen2-7b")
            .namespace("model-engine")
            .status("Running")
            .currentReplicas(2)
            .desiredReplicas(2)
            .framework("MindIE")
            .address("10.1.0.11:8080")
            .selector(Map.of("model_name", "qwen2-7b"))
            .labels(Map.of("model_name", "qwen2-7b", "framework", "MindIE"))
            .build()
    );

    @Override
    public List<InferenceInstance> listInstances() {
        log.debug("MockInferenceService.listInstances() → {} instances", INSTANCES.size());
        return INSTANCES;
    }

    @Override
    public Optional<InferenceInstance> getInstance(String instanceName) {
        return INSTANCES.stream()
            .filter(i -> i.getInstanceName().equals(instanceName))
            .findFirst();
    }

    @Override
    public List<InferenceInstance> listByNamespace(String namespace) {
        return INSTANCES.stream()
            .filter(i -> i.getNamespace().equals(namespace))
            .collect(Collectors.toList());
    }

    @Override
    public List<InferenceInstance> listByFramework(String framework) {
        return INSTANCES.stream()
            .filter(i -> i.getFramework().equalsIgnoreCase(framework))
            .collect(Collectors.toList());
    }

    @Override
    public List<InferenceInstance> listByStatus(String status) {
        return INSTANCES.stream()
            .filter(i -> i.getStatus().equalsIgnoreCase(status))
            .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String instanceName) {
        return INSTANCES.stream().anyMatch(i -> i.getInstanceName().equals(instanceName));
    }
}
