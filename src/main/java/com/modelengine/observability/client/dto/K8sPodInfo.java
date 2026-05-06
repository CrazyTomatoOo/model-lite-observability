package com.modelengine.observability.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO representing Kubernetes Pod information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sPodInfo {
    
    /**
     * Pod name
     */
    private String name;
    
    /**
     * Namespace
     */
    private String namespace;
    
    /**
     * Pod IP address
     */
    private String podIp;
    
    /**
     * Node name where the pod is running
     */
    private String nodeName;
    
    /**
     * Pod phase: Pending, Running, Succeeded, Failed, Unknown
     */
    private String phase;
    
    /**
     * Pod status
     */
    private String status;
    
    /**
     * Container images
     */
    private String image;
    
    /**
     * Pod labels
     */
    private Map<String, String> labels;
    
    /**
     * Creation timestamp
     */
    private Instant creationTimestamp;
    
    /**
     * Restart count
     */
    private Integer restartCount;
    
    /**
     * Ready container count
     */
    private Integer readyContainers;
    
    /**
     * Total container count
     */
    private Integer totalContainers;
    
    /**
     * Check if pod is running
     */
    public boolean isRunning() {
        return "Running".equals(phase);
    }
    
    /**
     * Check if pod is ready
     */
    public boolean isReady() {
        return "Running".equals(phase) && 
               readyContainers != null && 
               totalContainers != null && 
               readyContainers.equals(totalContainers);
    }
}
