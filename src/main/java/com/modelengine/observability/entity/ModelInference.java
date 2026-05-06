package com.modelengine.observability.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a ModelInference Kubernetes CRD resource.
 * ModelInference represents a model inference service deployed in K8s.
 *
 * @author ModelEngine Observability Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInference {

    /**
     * Unique service identifier (metadata.name)
     */
    private String serviceId;

    /**
     * Display name of the model inference service
     */
    private String name;

    /**
     * Kubernetes namespace
     */
    private String namespace;

    /**
     * Service status: Running, Pending, Failed, Unknown
     */
    private String status;

    /**
     * Number of desired replicas
     */
    private Integer replicas;

    /**
     * Number of ready replicas
     */
    private Integer readyReplicas;

    /**
     * Number of available replicas
     */
    private Integer availableReplicas;

    /**
     * Inference framework: MindIE, VLLM, SGLang
     */
    private String framework;

    /**
     * Framework version
     */
    private String frameworkVersion;

    /**
     * Label selector for matching Pods
     */
    private Map<String, String> selector;

    /**
     * Resource creation timestamp
     */
    private Instant createdAt;

    /**
     * Resource last update timestamp
     */
    private Instant updatedAt;

    /**
     * Additional labels
     */
    private Map<String, String> labels;

    /**
     * Additional annotations
     */
    private Map<String, String> annotations;
}
