package com.modelengine.observability.informer;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InformerManager {

    private final KubernetesClient kubernetesClient;
    private final ModelInferenceInformer modelInferenceInformer;
    private final PodInformer podInformer;

    @PostConstruct
    public void startAll() {
        log.info("Starting all Kubernetes informers...");
        try {
            modelInferenceInformer.initialize();
            podInformer.initialize();

            boolean synced = modelInferenceInformer.getInformer().hasSynced()
                    && podInformer.getInformer().hasSynced();

            if (synced) {
                log.info("All Kubernetes informers started and synced successfully");
            } else {
                log.warn("Some informers may not have fully synced yet, continuing...");
            }
        } catch (Exception e) {
            log.error("Failed to start Kubernetes informers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start Kubernetes informers", e);
        }
    }

    @PreDestroy
    public void stopAll() {
        log.info("Stopping all Kubernetes informers...");
        try {
            if (modelInferenceInformer.getInformer() != null) {
                modelInferenceInformer.getInformer().stop();
            }
            if (podInformer.getInformer() != null) {
                podInformer.getInformer().stop();
            }
            log.info("All Kubernetes informers stopped successfully");
        } catch (Exception e) {
            log.error("Error stopping Kubernetes informers: {}", e.getMessage(), e);
        }
    }

    public boolean hasSynced() {
        return modelInferenceInformer.getInformer() != null
                && modelInferenceInformer.getInformer().hasSynced()
                && podInformer.getInformer() != null
                && podInformer.getInformer().hasSynced();
    }

    public boolean waitForSync(long timeout, TimeUnit unit) {
        CompletableFuture<Void> modelFuture = modelInferenceInformer.getInformer() != null
                ? modelInferenceInformer.getInformer().start().toCompletableFuture()
                : CompletableFuture.completedFuture(null);
        CompletableFuture<Void> podFuture = podInformer.getInformer() != null
                ? podInformer.getInformer().start().toCompletableFuture()
                : CompletableFuture.completedFuture(null);

        try {
            CompletableFuture.allOf(modelFuture, podFuture).get(timeout, unit);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Error waiting for informer sync", e);
            return false;
        }
}

}