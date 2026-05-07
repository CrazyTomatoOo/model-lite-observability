package com.modelengine.observability.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.modelengine.observability.config.ObservabilityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache manager that wraps Caffeine cache with support for configurable TTL per entry.
 * Provides getOrLoad with custom loader and TTL, and manual invalidation capabilities.
 *
 * <p>Uses {@link ObservabilityProperties} to configure default maximum cache size.
 * Each cache entry can have its own TTL specified at load time.</p>
 */
@Slf4j
@Component("observabilityCacheManager")
public class CacheManager {

    private final Cache<String, CacheEntry<?>> cache;
    private final int defaultMaxSize;

    /**
     * Internal cache entry wrapper that stores the value and its expiration time.
     *
     * @param <T> the type of cached value
     */
    private record CacheEntry<T>(T value, long expireAtNanos) {
    }

    /**
     * Constructs a new CacheManager with configuration from ObservabilityProperties.
     *
     * @param properties the observability configuration properties
     */
    public CacheManager(ObservabilityProperties properties) {
        this.defaultMaxSize = properties.getCache().getMaxSize();
        this.cache = Caffeine.newBuilder()
                .maximumSize(defaultMaxSize)
                .recordStats()
                .build();
        log.info("CacheManager initialized with maxSize={}", defaultMaxSize);
    }

    /**
     * Gets a value from the cache, or loads it using the provided supplier if not present or expired.
     *
     * @param key    the cache key
     * @param loader the supplier to load the value if not cached
     * @param ttl    the time-to-live duration for this entry
     * @param <T>    the type of the cached value
     * @return the cached or newly loaded value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Supplier<T> loader, Duration ttl) {
        CacheEntry<T> entry = (CacheEntry<T>) cache.getIfPresent(key);

        if (entry != null && System.nanoTime() < entry.expireAtNanos()) {
            log.debug("Cache hit for key={}", key);
            return entry.value();
        }

        log.debug("Cache miss for key={}, loading value", key);
        T value = loader.get();
        if (value != null) {
            long expireAtNanos = System.nanoTime() + ttl.toNanos();
            cache.put(key, new CacheEntry<>(value, expireAtNanos));
        }
        return value;
    }

    /**
     * Gets a value from the cache, or loads it using the provided supplier if not present or expired.
     * Uses the default TTL from properties (metrics TTL).
     *
     * @param key    the cache key
     * @param loader the supplier to load the value if not cached
     * @param <T>    the type of the cached value
     * @return the cached or newly loaded value
     */
    public <T> T getOrLoad(String key, Supplier<T> loader) {
        return getOrLoad(key, loader, Duration.ofMinutes(5));
    }

    /**
     * Invalidates a specific cache entry by key.
     *
     * @param key the cache key to invalidate
     */
    public void invalidate(String key) {
        log.debug("Invalidating cache key={}", key);
        cache.invalidate(key);
    }

    /**
     * Invalidates all cache entries.
     */
    public void invalidateAll() {
        log.info("Invalidating all cache entries");
        cache.invalidateAll();
    }

    /**
     * Returns the estimated number of entries in the cache.
     *
     * @return the estimated cache size
     */
    public long estimatedSize() {
        return cache.estimatedSize();
    }
}
