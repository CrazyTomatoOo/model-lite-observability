package com.modelengine.observability.service.inference;

import java.util.List;
import java.util.Optional;

/** Abstraction layer over inference instance data sources. */
public interface InferenceService {

    /**
     * List all known inference instances.
     *
     * @return list of InferenceInstance, never null
     */
    List<InferenceInstance> listInstances();

    /**
     * Get a single inference instance by name.
     *
     * @param instanceName the instance name
     * @return Optional containing the instance, or empty if not found
     */
    Optional<InferenceInstance> getInstance(String instanceName);

    /**
     * List inference instances filtered by namespace.
     *
     * @param namespace the namespace to filter by
     * @return list of matching InferenceInstance, never null
     */
    List<InferenceInstance> listByNamespace(String namespace);

    /**
     * List inference instances filtered by framework.
     *
     * @param framework the framework to filter by (MindIE, VLLM, SGLang)
     * @return list of matching InferenceInstance, never null
     */
    List<InferenceInstance> listByFramework(String framework);

    /**
     * List inference instances filtered by status.
     *
     * @param status the status to filter by (Running, Pending, Failed, Unknown)
     * @return list of matching InferenceInstance, never null
     */
    List<InferenceInstance> listByStatus(String status);

    /**
     * Check if an inference instance exists.
     *
     * @param instanceName the instance name
     * @return true if the instance exists
     */
    boolean exists(String instanceName);
}
