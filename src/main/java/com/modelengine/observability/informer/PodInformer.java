package com.modelengine.observability.informer;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Watches Pod resources in Kubernetes.
 * Uses the informer's built-in indexer/store via Lister for queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PodInformer {

    private final KubernetesClient kubernetesClient;

    @Getter
    private SharedIndexInformer<Pod> informer;

    /**
     * Lister backed by the informer's built-in indexer/store.
     */
    private Lister<Pod> lister;

    /**
     * Initialize the informer.
     */
    void initialize() {
        log.info("Initializing Pod informer...");

        this.informer = kubernetesClient.pods()
                .inform(new ResourceEventHandler<Pod>() {
                    @Override
                    public void onAdd(Pod pod) {
                        log.debug("Pod added: {}/{}",
                                pod.getMetadata().getNamespace(),
                                pod.getMetadata().getName());
                    }

                    @Override
                    public void onUpdate(Pod oldPod, Pod newPod) {
                        log.debug("Pod updated: {}/{}",
                                newPod.getMetadata().getNamespace(),
                                newPod.getMetadata().getName());
                    }

                    @Override
                    public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                        log.debug("Pod deleted: {}/{}",
                                pod.getMetadata().getNamespace(),
                                pod.getMetadata().getName());
                    }
                });

        this.lister = new Lister<>(informer.getIndexer());

        log.info("Pod informer initialized");
    }


    public Pod getPod(String podName) {
        return lister.list().stream()
                .filter(p -> Objects.equals(p.getMetadata().getName(), podName))
                .findFirst()
                .orElse(null);
    }

    public List<Pod> listPods() {
        return List.copyOf(lister.list());
    }

    public List<Pod> listByNamespace(String namespace) {
        return lister.namespace(namespace).list();
    }

    public List<Pod> listBySelector(Map<String, String> selector) {
        if (selector == null || selector.isEmpty()) {
            return List.copyOf(lister.list());
        }

        return lister.list().stream()
                .filter(pod -> matchesSelector(pod, selector))
                .collect(Collectors.toList());
    }

    private boolean matchesSelector(Pod pod, Map<String, String> selector) {
        Map<String, String> podLabels = pod.getMetadata().getLabels();
        if (podLabels == null) {
            return selector.isEmpty();
        }

        for (Map.Entry<String, String> entry : selector.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String podValue = podLabels.get(key);
            if (!Objects.equals(value, podValue)) {
                return false;
            }
        }
        return true;
    }

    public int getCacheSize() {
        return lister.list().size();
    }

    public boolean contains(String podName) {
        return lister.list().stream()
                .anyMatch(p -> Objects.equals(p.getMetadata().getName(), podName));
    }

    public Collection<Pod> getCachedValues() {
        return lister.list();
    }
}
