package com.modelengine.observability.service.inference;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mock implementation of InferenceService returning hardcoded instances.
 * Provides 30 model instances with MindIE framework to trigger pagination.
 */
@Slf4j
@Service
@Primary
public class MockInferenceService implements InferenceService {

    static final List<InferenceInstance> INSTANCES = List.of(
        // Large production models
        createInstance("llama3-70b", "model-engine", InstanceStatus.RUNNING, "MindIE", 4, 4, "10.1.0.10:8080"),
        createInstance("llama3-8b", "model-engine", InstanceStatus.RUNNING, "MindIE", 2, 2, "10.1.0.11:8080"),
        createInstance("qwen2-7b", "model-engine", InstanceStatus.RUNNING, "MindIE", 2, 2, "10.1.0.12:8080"),
        createInstance("qwen2-72b", "model-engine", InstanceStatus.PENDING, "MindIE", 0, 4, "10.1.0.15:8080"),
        createInstance("qwen2-110b", "model-engine", InstanceStatus.RUNNING, "MindIE", 8, 8, "10.1.0.13:8080"),
        createInstance("baichuan2-13b", "model-engine", InstanceStatus.RUNNING, "MindIE", 2, 2, "10.1.0.14:8080"),

        // Research models
        createInstance("chatglm3-6b", "research", InstanceStatus.FAILED, "MindIE", 0, 2, "10.1.0.15:8080"),
        createInstance("Yi-34b", "research", InstanceStatus.RUNNING, "MindIE", 4, 4, "10.1.0.16:8080"),
        createInstance("mistral-7b", "research", InstanceStatus.RUNNING, "MindIE", 2, 2, "10.1.0.17:8080"),
        createInstance("mixtral-8x7b", "research", InstanceStatus.PENDING, "MindIE", 0, 8, "10.1.0.15:8080"),

        // Edge models
        createInstance("phi-3-mini", "edge", InstanceStatus.RUNNING, "MindIE", 1, 1, "10.1.0.18:8080"),
        createInstance("gemma-2b", "edge", InstanceStatus.RUNNING, "MindIE", 1, 1, "10.1.0.19:8080"),
        createInstance("qwen2-1.5b", "edge", InstanceStatus.TERMINATING, "MindIE", 1, 0, "10.1.0.20:8080"),
        createInstance("tinyllama-1.1b", "edge", InstanceStatus.UNKNOWN, "MindIE", 0, 1, "10.1.0.15:8080"),

        // Production namespace - diverse statuses
        createInstance("gpt-j-6b", "production", InstanceStatus.RUNNING, "MindIE", 3, 3, "10.1.0.21:8080"),
        createInstance("falcon-40b", "production", InstanceStatus.RUNNING, "MindIE", 4, 4, "10.1.0.22:8080"),
        createInstance("codellama-34b", "production", InstanceStatus.RUNNING, "MindIE", 3, 3, "10.1.0.23:8080"),
        createInstance("bloom-7b1", "production", InstanceStatus.FAILED, "MindIE", 0, 2, "10.1.0.15:8080"),
        createInstance("starcoder2-15b", "production", InstanceStatus.PENDING, "MindIE", 0, 2, "10.1.0.15:8080"),

        // Special purpose
        createInstance("whisper-large-v3", "audio", InstanceStatus.RUNNING, "MindIE", 2, 2, "10.1.0.24:8080"),
        createInstance("clip-vit-large", "vision", InstanceStatus.RUNNING, "MindIE", 1, 1, "10.1.0.25:8080"),
        createInstance("bert-base-chinese", "nlp", InstanceStatus.SUCCEEDED, "MindIE", 0, 0, "10.1.0.15:8080"),
        createInstance("roberta-large", "nlp", InstanceStatus.RUNNING, "MindIE", 1, 1, "10.1.0.26:8080"),
        createInstance("m3e-base", "embedding", InstanceStatus.RUNNING, "MindIE", 2, 2, "10.1.0.27:8080"),
        createInstance("bge-large-zh", "embedding", InstanceStatus.PENDING, "MindIE", 0, 2, "10.1.0.15:8080"),
        createInstance("jina-embeddings-v2", "embedding", InstanceStatus.RUNNING, "MindIE", 1, 1, "10.1.0.28:8080")
    );

    private static InferenceInstance createInstance(String name, String namespace,
                                                     InstanceStatus status, String framework,
                                                     int currentReplicas, int desiredReplicas,
                                                     String address) {
        return InferenceInstance.builder()
                .instanceName(name)
                .namespace(namespace)
                .status(status)
                .currentReplicas(currentReplicas)
                .desiredReplicas(desiredReplicas)
                .framework(framework)
                .address(address)
                .selector(Map.of("model_name", name))
                .labels(Map.of("model_name", name, "framework", framework))
                .annotations(Map.of("dme.modelengine.io/owner", "ml-platform", "dme.modelengine.io/version", "1.0.0"))
                .build();
    }

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
        InstanceStatus targetStatus = InstanceStatus.fromString(status);
        return INSTANCES.stream()
                .filter(i -> i.getStatus() == targetStatus)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String instanceName) {
        return INSTANCES.stream().anyMatch(i -> i.getInstanceName().equals(instanceName));
    }
}
