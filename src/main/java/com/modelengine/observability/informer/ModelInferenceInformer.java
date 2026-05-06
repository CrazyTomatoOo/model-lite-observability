package com.modelengine.observability.informer;

import com.modelengine.observability.entity.ModelInference;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Watches ModelInference CRD resources in Kubernetes.
 * Uses the informer's built-in indexer/store via Lister for queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelInferenceInformer {

    private static final String MODEL_INFERENCE_GROUP = "modelengine.io";
    private static final String MODEL_INFERENCE_VERSION = "v1";
    private static final String MODEL_INFERENCE_PLURAL = "modelinferences";

    private final KubernetesClient kubernetesClient;

    @Getter
    private SharedIndexInformer<GenericKubernetesResource> informer;

    /**
     * Lister backed by the informer's built-in indexer/store.
     * Provides O(1) namespace-filtered lookups and iteration.
     */
    private Lister<GenericKubernetesResource> lister;

    /**
     * Initialize the informer.
     */
    void initialize() {
        log.info("Initializing ModelInference informer...");

        ResourceDefinitionContext context = new ResourceDefinitionContext.Builder()
                .withGroup(MODEL_INFERENCE_GROUP)
                .withVersion(MODEL_INFERENCE_VERSION)
                .withPlural(MODEL_INFERENCE_PLURAL)
                .withNamespaced(true)
                .build();

        this.informer = kubernetesClient.genericKubernetesResources(context)
                .inform(new ResourceEventHandler<GenericKubernetesResource>() {
                    @Override
                    public void onAdd(GenericKubernetesResource resource) {
                        log.debug("ModelInference added: {}/{}",
                                resource.getMetadata().getNamespace(),
                                resource.getMetadata().getName());
                    }

                    @Override
                    public void onUpdate(GenericKubernetesResource oldResource, GenericKubernetesResource newResource) {
                        log.debug("ModelInference updated: {}/{}",
                                newResource.getMetadata().getNamespace(),
                                newResource.getMetadata().getName());
                    }

                    @Override
                    public void onDelete(GenericKubernetesResource resource, boolean deletedFinalStateUnknown) {
                        log.debug("ModelInference deleted: {}/{}",
                                resource.getMetadata().getNamespace(),
                                resource.getMetadata().getName());
                    }
                });

        this.lister = new Lister<>(informer.getIndexer());

        log.info("ModelInference informer initialized");
    }

    /**
     * Convert a GenericKubernetesResource to ModelInference entity.
     *
     * @param resource the GenericKubernetesResource
     * @return the ModelInference entity
     */
    @SuppressWarnings("unchecked")
    private ModelInference convertToModelInference(GenericKubernetesResource resource) {
        Map<String, Object> additionalProperties = resource.getAdditionalProperties();
        Map<String, Object> spec = (Map<String, Object>) additionalProperties.getOrDefault("spec", Map.of());
        Map<String, Object> status = (Map<String, Object>) additionalProperties.getOrDefault("status", Map.of());

        String serviceId = resource.getMetadata().getName();
        String namespace = resource.getMetadata().getNamespace();

        // Parse spec fields
        String name = getStringValue(spec, "name", serviceId);
        String framework = getStringValue(spec, "framework", "Unknown");
        String frameworkVersion = getStringValue(spec, "frameworkVersion", "");
        Integer replicas = getIntegerValue(spec, "replicas", 0);
        Map<String, String> selector = getMapValue(spec, "selector");

        // Parse status fields
        String serviceStatus = getStringValue(status, "phase", "Unknown");
        Integer readyReplicas = getIntegerValue(status, "readyReplicas", 0);
        Integer availableReplicas = getIntegerValue(status, "availableReplicas", 0);

        // Parse timestamps
        Instant createdAt = parseInstant(resource.getMetadata().getCreationTimestamp());
        Instant updatedAt = parseInstant(resource.getMetadata().getCreationTimestamp());

        return ModelInference.builder()
                .serviceId(serviceId)
                .name(name)
                .namespace(namespace)
                .status(serviceStatus)
                .replicas(replicas)
                .readyReplicas(readyReplicas)
                .availableReplicas(availableReplicas)
                .framework(framework)
                .frameworkVersion(frameworkVersion)
                .selector(selector)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .labels(resource.getMetadata().getLabels())
                .annotations(resource.getMetadata().getAnnotations())
                .build();
    }

    /**
     * Retrieve a ModelInference from cache by serviceId.
     *
     * @param serviceId the service identifier
     * @return the ModelInference, or null if not found
     */
    public ModelInference getModelInference(String serviceId) {
        return lister.list().stream()
                .filter(r -> Objects.equals(r.getMetadata().getName(), serviceId))
                .findFirst()
                .map(this::convertToModelInference)
                .orElse(null);
    }

    /**
     * List all cached ModelInference resources.
     *
     * @return list of all ModelInferences
     */
    public List<ModelInference> listModelInferences() {
        return lister.list().stream()
                .map(this::convertToModelInference)
                .collect(Collectors.toList());
    }

    /**
     * List ModelInferences filtered by namespace.
     *
     * @param namespace the namespace to filter by
     * @return list of matching ModelInferences
     */
    public List<ModelInference> listByNamespace(String namespace) {
        return lister.namespace(namespace).list().stream()
                .map(this::convertToModelInference)
                .collect(Collectors.toList());
    }

    /**
     * List ModelInferences filtered by framework.
     *
     * @param framework the framework to filter by (MindIE, VLLM, SGLang)
     * @return list of matching ModelInferences
     */
    public List<ModelInference> listByFramework(String framework) {
        return lister.list().stream()
                .map(this::convertToModelInference)
                .filter(mi -> Objects.equals(mi.getFramework(), framework))
                .collect(Collectors.toList());
    }

    /**
     * List ModelInferences filtered by status.
     *
     * @param status the status to filter by (Running, Pending, Failed, Unknown)
     * @return list of matching ModelInferences
     */
    public List<ModelInference> listByStatus(String status) {
        return lister.list().stream()
                .map(this::convertToModelInference)
                .filter(mi -> Objects.equals(mi.getStatus(), status))
                .collect(Collectors.toList());
    }

    /**
     * Get the current cache size.
     *
     * @return number of cached ModelInferences
     */
    public int getCacheSize() {
        return lister.list().size();
    }

    /**
     * Check if the cache contains a service.
     *
     * @param serviceId the service identifier
     * @return true if present in cache
     */
    public boolean contains(String serviceId) {
        return lister.list().stream()
                .anyMatch(r -> Objects.equals(r.getMetadata().getName(), serviceId));
    }

    /**
     * Get all cached values.
     *
     * @return collection of all ModelInferences
     */
    public Collection<ModelInference> getCachedValues() {
        return lister.list().stream()
                .map(this::convertToModelInference)
                .collect(Collectors.toList());
    }

    // Helper methods

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMapValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            try {
                return (Map<String, String>) value;
            } catch (ClassCastException e) {
                return Map.of();
            }
        }
        return Map.of();
    }

    private Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
