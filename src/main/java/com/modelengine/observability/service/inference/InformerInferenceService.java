package com.modelengine.observability.service.inference;

import com.modelengine.observability.informer.ModelInferenceInformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Real implementation of InferenceService backed by {@link ModelInferenceInformer}.
 * Converts ModelInference entities to InferenceInstance DTOs.
 * Active when the "stub" profile is NOT set.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!stub")
public class InformerInferenceService implements InferenceService {

    private final ModelInferenceInformer informer;

    @Override
    public List<InferenceInstance> listInstances() {
        log.debug("InformerInferenceService.listInstances() called");
        return informer.listModelInferences().stream()
                .map(this::toInstance)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InferenceInstance> getInstance(String instanceName) {
        log.debug("InformerInferenceService.getInstance({}) called", instanceName);
        return Optional.ofNullable(informer.getModelInference(instanceName))
                .map(this::toInstance);
    }

    @Override
    public List<InferenceInstance> listByNamespace(String namespace) {
        log.debug("InformerInferenceService.listByNamespace({}) called", namespace);
        return informer.listByNamespace(namespace).stream()
                .map(this::toInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<InferenceInstance> listByFramework(String framework) {
        log.debug("InformerInferenceService.listByFramework({}) called", framework);
        return informer.listByFramework(framework).stream()
                .map(this::toInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<InferenceInstance> listByStatus(String status) {
        log.debug("InformerInferenceService.listByStatus({}) called", status);
        return informer.listByStatus(status).stream()
                .map(this::toInstance)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String instanceName) {
        log.debug("InformerInferenceService.exists({}) called", instanceName);
        return informer.contains(instanceName);
    }

    private InferenceInstance toInstance(com.modelengine.observability.entity.ModelInference mi) {
        return InferenceInstance.builder()
                .instanceName(mi.getServiceId())
                .namespace(mi.getNamespace())
                .status(mi.getStatus())
                .currentReplicas(mi.getAvailableReplicas())
                .desiredReplicas(mi.getReplicas())
                .selector(mi.getSelector())
                .framework(mi.getFramework())
                .address(null) // not available from CRD yet
                .labels(mi.getLabels())
                .annotations(mi.getAnnotations())
                .build();
    }
}
