package com.modelengine.observability.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO representing Kubernetes Service information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sServiceInfo {
    
    /**
     * Service name
     */
    private String name;
    
    /**
     * Namespace
     */
    private String namespace;
    
    /**
     * Service type: ClusterIP, NodePort, LoadBalancer, ExternalName
     */
    private String type;
    
    /**
     * Cluster IP
     */
    private String clusterIp;
    
    /**
     * External IPs
     */
    private String externalIps;
    
    /**
     * Service port
     */
    private Integer port;
    
    /**
     * Target port
     */
    private String targetPort;
    
    /**
     * Service selector labels
     */
    private Map<String, String> selector;
    
    /**
     * Service labels
     */
    private Map<String, String> labels;
    
    /**
     * Creation timestamp
     */
    private Instant creationTimestamp;
    
    /**
     * Check if service is a model inference service
     */
    public boolean isModelInferenceService() {
        return labels != null && 
               (labels.containsKey("modelengine.io/service-type") || 
                labels.containsKey("app.kubernetes.io/component"));
    }
}
