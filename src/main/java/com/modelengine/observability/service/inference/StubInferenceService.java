package com.modelengine.observability.service.inference;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Stub implementation of InferenceService for development/testing.
 * Returns empty results for all queries. Active when the "stub" Spring profile is set.
 */
@Slf4j
@Service
@Profile("stub")
public class StubInferenceService implements InferenceService {

    @Override
    public List<InferenceInstance> listInstances() {
        log.debug("StubInferenceService.listInstances() called");
        return Collections.emptyList();
    }

    @Override
    public Optional<InferenceInstance> getInstance(String instanceName) {
        log.debug("StubInferenceService.getInstance({}) called", instanceName);
        return Optional.empty();
    }

    @Override
    public List<InferenceInstance> listByNamespace(String namespace) {
        log.debug("StubInferenceService.listByNamespace({}) called", namespace);
        return Collections.emptyList();
    }

    @Override
    public List<InferenceInstance> listByFramework(String framework) {
        log.debug("StubInferenceService.listByFramework({}) called", framework);
        return Collections.emptyList();
    }

    @Override
    public List<InferenceInstance> listByStatus(String status) {
        log.debug("StubInferenceService.listByStatus({}) called", status);
        return Collections.emptyList();
    }

    @Override
    public boolean exists(String instanceName) {
        log.debug("StubInferenceService.exists({}) called", instanceName);
        return false;
    }
}
