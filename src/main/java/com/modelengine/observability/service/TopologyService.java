package com.modelengine.observability.service;

import com.modelengine.observability.informer.PodInformer;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for resolving inference instances to their underlying Pods.
 * <p>
 * Delegates instance lookup to {@link InferenceService} and pod lookup
 * to the {@link PodInformer} cache.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopologyService {

    private final InferenceService inferenceService;
    private final PodInformer podInformer;

    /**
     * Gets the list of Pods for a given inference instance.
     * Convenience method used by MetricsService.
     *
     * @param instanceName the inference instance name
     * @return list of matching Pods, or empty list if instance not found
     */
    public List<Pod> getPodsForInstance(String instanceName) {
        log.debug("Getting pods for instance: {}", instanceName);

        return inferenceService.getInstance(instanceName)
                .map(InferenceInstance::getSelector)
                .map(podInformer::listBySelector)
                .orElseGet(() -> {
                    log.warn("Inference instance not found: {}", instanceName);
                    return List.of();
                });
    }
}