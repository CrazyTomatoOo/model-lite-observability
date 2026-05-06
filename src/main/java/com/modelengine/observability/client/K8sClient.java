package com.modelengine.observability.client;

import com.modelengine.observability.client.dto.K8sPodInfo;
import com.modelengine.observability.client.dto.K8sServiceInfo;

import java.util.List;
import java.util.Map;

/**
 * Client for interacting with Kubernetes API.
 */
public interface K8sClient {
    
    /**
     * Initialize the Kubernetes client connection.
     */
    void connect();
    
    /**
     * List pods in a namespace filtered by labels.
     *
     * @param namespace namespace to search in
     * @param labels    label selector (key-value pairs)
     * @return list of matching pods
     */
    List<K8sPodInfo> listPods(String namespace, Map<String, String> labels);
    
    /**
     * Get details of a single pod.
     *
     * @param namespace namespace
     * @param podName   pod name
     * @return pod info or null if not found
     */
    K8sPodInfo getPod(String namespace, String podName);
    
    /**
     * List services in a namespace.
     *
     * @param namespace namespace to search in
     * @return list of services
     */
    List<K8sServiceInfo> listServices(String namespace);
    
    /**
     * Check connectivity to Kubernetes API.
     *
     * @return true if K8s API is reachable
     */
    boolean ping();
    
    /**
     * Close the client and release resources.
     */
    void close();
}
