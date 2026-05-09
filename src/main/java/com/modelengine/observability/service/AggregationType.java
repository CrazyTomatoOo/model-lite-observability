package com.modelengine.observability.service;

/**
 * Aggregation types supported for metric aggregation.
 * <p>
 * <ul>
 *   <li><b>AVG</b>: Simple arithmetic mean across all pods</li>
 *   <li><b>SUM</b>: Sum of all pod values</li>
 *   <li><b>WEIGHTED_AVG</b>: Weighted average (e.g., for success rates weighted by request count)</li>
 * </ul>
 * </p>
 */
public enum AggregationType {
    AVG,
    SUM,
    WEIGHTED_AVG
}
