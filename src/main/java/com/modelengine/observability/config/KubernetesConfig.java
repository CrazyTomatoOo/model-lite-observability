package com.modelengine.observability.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kubernetes client configuration.
 * Provides a shared KubernetesClient bean for the application.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KubernetesConfig {

    private final ObservabilityProperties properties;

    /**
     * Creates and configures the Kubernetes client.
     * Supports in-cluster config and kubeconfig-based authentication.
     *
     * @return the configured KubernetesClient
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        try {
            Config config;
            String configPath = properties.getKubernetes().getConfigPath();

            if (configPath != null && !configPath.isEmpty()) {
                log.info("Connecting to Kubernetes using kubeconfig: {}", configPath);
                config = Config.fromKubeconfig(configPath);
            } else if (properties.getKubernetes().isInCluster()) {
                log.info("Connecting to Kubernetes using in-cluster config");
                config = new ConfigBuilder().build();
            } else {
                log.info("Connecting to Kubernetes using default kubeconfig");
                config = new ConfigBuilder().build();
            }

            int connectionTimeout = (int) properties.getKubernetes().getConnectionTimeout().toMillis();
            int requestTimeout = (int) properties.getKubernetes().getRequestTimeout().toMillis();
            config.setConnectionTimeout(connectionTimeout);
            config.setRequestTimeout(requestTimeout);

            KubernetesClient client = new KubernetesClientBuilder()
                    .withConfig(config)
                    .build();

            String version = client.getKubernetesVersion().getGitVersion();
            log.info("Connected to Kubernetes cluster version: {}", version);

            return client;
        } catch (Exception e) {
            log.error("Failed to connect to Kubernetes: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Kubernetes client", e);
        }
    }
}
