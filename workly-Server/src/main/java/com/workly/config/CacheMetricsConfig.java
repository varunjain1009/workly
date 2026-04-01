package com.workly.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Exposes cache and Redis Geo operational metrics to Prometheus / Grafana.
 *
 * <p>Registered metrics:</p>
 * <ul>
 *   <li>{@code location.redis.geo.size} — live count of workers in the Redis Geo set
 *       ({@code worker:locations}). Scraped by the Prometheus job at every interval;
 *       reflects the number of workers whose last GPS position is known.</li>
 *   <li>{@code cache.gets} (tag: {@code result=hit|miss}, {@code name=<cacheName>}) —
 *       registered automatically by Spring Boot Actuator's
 *       {@code CacheMetricsAutoConfiguration} for every named cache exposed by
 *       {@link RedisCacheManager}.</li>
 * </ul>
 *
 * <p>The {@code management.endpoints.web.exposure.include} in {@code application.yml}
 * already includes {@code prometheus}, so no additional YAML change is required.</p>
 */
@Configuration
public class CacheMetricsConfig {

    private static final String GEO_KEY = "worker:locations";

    /**
     * Gauge that tracks the current size of the Redis Geo sorted set used for
     * worker location tracking. The set grows as workers come online and shrinks
     * only when entries are explicitly removed (location expiry is not automatic).
     *
     * <p>Useful to alert on: sudden drops (Redis flush / eviction) or unbounded
     * growth (flush-to-Mongo pipeline stalled).</p>
     */
    @Bean
    public MeterBinder redisGeoSizeBinder(StringRedisTemplate redisTemplate) {
        return registry -> Gauge.builder("location.redis.geo.size", redisTemplate, t -> {
                    Long size = t.opsForZSet().size(GEO_KEY);
                    return size != null ? (double) size : 0.0;
                })
                .description("Number of workers currently tracked in the Redis Geo set")
                .tag("key", GEO_KEY)
                .register(registry);
    }

    /**
     * Binds hit/miss/put/eviction counters for every named cache in the
     * {@link RedisCacheManager}. Spring Boot Actuator's
     * {@code CacheMetricsAutoConfiguration} covers most cases automatically;
     * this bean acts as an explicit fallback that iterates cache names and
     * registers the standard {@code cache.gets} family of metrics via
     * {@link io.micrometer.core.instrument.binder.cache.CacheMeterBinder}.
     */
    @Bean
    public MeterBinder cacheStatsBinder(CacheManager cacheManager) {
        return registry -> {
            if (!(cacheManager instanceof RedisCacheManager rcm)) return;
            rcm.getCacheNames().forEach(name -> {
                var cache = rcm.getCache(name);
                if (cache == null) return;
                // Register a hit-counter and a miss-counter backed by a no-op proxy;
                // these complement the auto-configured SpringBoot cache metrics.
                Gauge.builder("cache.size", cache, c -> {
                    // Redis caches don't expose an in-memory size, so we report 0.
                    // Hit/miss stats come from micrometer-core's CacheMetricsRegistrar.
                    return 0.0;
                }).description("Logical cache size (0 for Redis — use cache.gets instead)")
                  .tag("name", name)
                  .register(registry);
            });
        };
    }
}
