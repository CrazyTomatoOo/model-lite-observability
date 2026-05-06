package com.modelengine.observability.service.inference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing an inference service instance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceInstance {

    /** Maps from ModelInference.serviceId */
    private String instanceName;
    private String namespace;
    private String status;
    private Integer currentReplicas;
    private Integer desiredReplicas;
    private Map<String, String> selector;
    private String framework;
    /** Null until CRD provides it */
    private String address;
    private Map<String, String> labels;
    private Map<String, String> annotations;
}
