package com.modelengine.observability.client;

import com.modelengine.observability.client.dto.K8sPodInfo;
import com.modelengine.observability.client.dto.K8sServiceInfo;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link K8sClient} using fabric8 Kubernetes client.
 * Supports in-cluster and kubeconfig-based authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class K8sClientImpl implements K8sClient {

    private final KubernetesClient client;

    @Override
    public void connect() {
        // Connection is established during bean creation in KubernetesConfig
        log.debug("K8sClient connect called - client already initialized");
    }

    @Override
    public List<K8sPodInfo> listPods(String namespace, Map<String, String> labels) {
        log.debug("Listing pods in namespace: {} with labels: {}", namespace, labels);
        try {
            var podList = client.pods()
                .inNamespace(namespace)
                .withLabels(labels != null ? labels : Collections.emptyMap())
                .list();

            return podList.getItems().stream()
                .map(this::toK8sPodInfo)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list pods in namespace {}: {}", namespace, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public K8sPodInfo getPod(String namespace, String podName) {
        log.debug("Getting pod: {}/{}", namespace, podName);
        try {
            Pod pod = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .get();
            return pod != null ? toK8sPodInfo(pod) : null;
        } catch (Exception e) {
            log.error("Failed to get pod {}/{}: {}", namespace, podName, e.getMessage());
            return null;
        }
    }

    @Override
    public List<K8sServiceInfo> listServices(String namespace) {
        log.debug("Listing services in namespace: {}", namespace);
        try {
            var serviceList = client.services()
                .inNamespace(namespace)
                .list();

            return serviceList.getItems().stream()
                .map(this::toK8sServiceInfo)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list services in namespace {}: {}", namespace, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean ping() {
        try {
            if (client == null) {
                return false;
            }
            client.pods().inNamespace("kube-system").list();
            log.debug("Kubernetes API health check: UP");
            return true;
        } catch (Exception e) {
            log.warn("Kubernetes API health check failed: {}", e.getMessage());
            return false;
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (client != null) {
            log.info("Closing Kubernetes client");
            client.close();
        }
    }

    private K8sPodInfo toK8sPodInfo(Pod pod) {
        K8sPodInfo info = new K8sPodInfo();
        info.setName(pod.getMetadata().getName());
        info.setNamespace(pod.getMetadata().getNamespace());
        info.setLabels(pod.getMetadata().getLabels());

        if (pod.getStatus() != null) {
            PodStatus status = pod.getStatus();
            info.setPhase(status.getPhase());
            info.setPodIp(status.getPodIP());
            info.setNodeName(status.getHostIP());

            if (status.getConditions() != null && !status.getConditions().isEmpty()) {
                info.setStatus(status.getConditions().get(0).getType() + "=" + status.getConditions().get(0).getStatus());
            }

            if (status.getContainerStatuses() != null && !status.getContainerStatuses().isEmpty()) {
                var containerStatus = status.getContainerStatuses().get(0);
                info.setRestartCount(containerStatus.getRestartCount());
                info.setReadyContainers(containerStatus.getReady() ? 1 : 0);
                info.setTotalContainers(1);
            }
        }

        if (pod.getSpec() != null && pod.getSpec().getContainers() != null && !pod.getSpec().getContainers().isEmpty()) {
            info.setImage(pod.getSpec().getContainers().get(0).getImage());
            info.setTotalContainers(pod.getSpec().getContainers().size());
        }

        if (pod.getMetadata().getCreationTimestamp() != null) {
            info.setCreationTimestamp(Instant.parse(pod.getMetadata().getCreationTimestamp()));
        }

        return info;
    }

    private K8sServiceInfo toK8sServiceInfo(Service service) {
        K8sServiceInfo info = new K8sServiceInfo();
        info.setName(service.getMetadata().getName());
        info.setNamespace(service.getMetadata().getNamespace());
        info.setLabels(service.getMetadata().getLabels());

        ServiceSpec spec = service.getSpec();
        if (spec != null) {
            info.setType(spec.getType());
            info.setClusterIp(spec.getClusterIP());
            info.setSelector(spec.getSelector());

            if (spec.getPorts() != null && !spec.getPorts().isEmpty()) {
                var port = spec.getPorts().get(0);
                info.setPort(port.getPort());
                info.setTargetPort(port.getTargetPort().getStrVal());
            }
        }

        if (service.getMetadata().getCreationTimestamp() != null) {
            info.setCreationTimestamp(Instant.parse(service.getMetadata().getCreationTimestamp()));
        }

        return info;
    }
}
