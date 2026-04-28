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
        manager.registerCustomCache(CacheNames.CURRENT_EMPLOYEE, Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(500)
                .build());
        manager.registerCustomCache(CacheNames.WORKFLOW_RULES, Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build());
        manager.registerCustomCache(CacheNames.HOLIDAY_CALENDAR, Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(200)
                .build());
        manager.registerCustomCache(CacheNames.EMPLOYEE_SCHEDULE, Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build());
        manager.registerCustomCache(CacheNames.COMPANY_WORK_SCHEDULE, Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build());
        return manager;
    }
}
