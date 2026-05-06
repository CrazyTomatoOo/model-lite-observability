package com.modelengine.observability.service;

import com.modelengine.observability.informer.PodInformer;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopologyServiceTest {

    @Mock
    private InferenceService inferenceService;

    @Mock
    private PodInformer podInformer;

    @InjectMocks
    private TopologyService topologyService;

    @Test
    void getPodsForInstanceReturnsPodsWhenInstanceFound() {
        String instanceName = "test-instance";
        Map<String, String> selector = Map.of("app", "test-app");
        InferenceInstance instance = InferenceInstance.builder()
                .instanceName(instanceName)
                .selector(selector)
                .build();
        Pod pod1 = mock(Pod.class);
        Pod pod2 = mock(Pod.class);
        List<Pod> expectedPods = List.of(pod1, pod2);

        when(inferenceService.getInstance(instanceName)).thenReturn(Optional.of(instance));
        when(podInformer.listBySelector(selector)).thenReturn(expectedPods);

        List<Pod> result = topologyService.getPodsForInstance(instanceName);

        assertEquals(expectedPods, result);
        verify(inferenceService).getInstance(instanceName);
        verify(podInformer).listBySelector(selector);
    }

    @Test
    void getPodsForInstanceReturnsEmptyWhenInstanceNotFound() {
        String instanceName = "non-existent-instance";

        when(inferenceService.getInstance(instanceName)).thenReturn(Optional.empty());

        List<Pod> result = topologyService.getPodsForInstance(instanceName);

        assertTrue(result.isEmpty());
        verify(inferenceService).getInstance(instanceName);
        verifyNoInteractions(podInformer);
    }

    @Test
    void getPodsForInstanceReturnsEmptyWhenSelectorIsNull() {
        String instanceName = "test-instance";
        InferenceInstance instance = InferenceInstance.builder()
                .instanceName(instanceName)
                .selector(null)
                .build();

        when(inferenceService.getInstance(instanceName)).thenReturn(Optional.of(instance));

        List<Pod> result = topologyService.getPodsForInstance(instanceName);

        assertTrue(result.isEmpty());
        verify(inferenceService).getInstance(instanceName);
    }

    @Test
    void getPodsForInstanceUsesInstanceNameParameter() {
        // Verify the method accepts instanceName (not serviceId) by passing a name
        String instanceName = "instance-by-name";
        Map<String, String> selector = Map.of("app", "test");
        InferenceInstance instance = InferenceInstance.builder()
                .instanceName(instanceName)
                .selector(selector)
                .build();
        Pod pod = mock(Pod.class);

        when(inferenceService.getInstance(instanceName)).thenReturn(Optional.of(instance));
        when(podInformer.listBySelector(selector)).thenReturn(List.of(pod));

        List<Pod> result = topologyService.getPodsForInstance(instanceName);

        assertEquals(1, result.size());
        verify(inferenceService).getInstance(instanceName);
    }
}
