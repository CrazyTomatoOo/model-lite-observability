package com.modelengine.observability.service.inference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StubInferenceServiceTest {

    private final StubInferenceService service = new StubInferenceService();

    @Test
    void listInstancesReturnsEmptyList() {
        assertTrue(service.listInstances().isEmpty());
    }

    @Test
    void getInstanceReturnsEmptyOptional() {
        assertTrue(service.getInstance("any-instance").isEmpty());
    }

    @Test
    void listByNamespaceReturnsEmptyList() {
        assertTrue(service.listByNamespace("any-namespace").isEmpty());
    }

    @Test
    void listByFrameworkReturnsEmptyList() {
        assertTrue(service.listByFramework("any-framework").isEmpty());
    }

    @Test
    void listByStatusReturnsEmptyList() {
        assertTrue(service.listByStatus("any-status").isEmpty());
    }

    @Test
    void existsReturnsFalse() {
        assertFalse(service.exists("any-instance"));
    }
}
