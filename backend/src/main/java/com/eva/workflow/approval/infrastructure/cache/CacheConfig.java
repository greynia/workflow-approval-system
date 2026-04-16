package com.eva.workflow.approval.infrastructure.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Dynamic cache manager: caches are created on first use.
     * All caches share the same default spec (30s TTL, max 500 entries).
     * To give a specific cache a different spec, switch to explicit registration
     * and call manager.registerCustomCache(name, caffeine.build()) per cache.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(500));
        return manager;
    }
}
