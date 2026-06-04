package com.demo.payment.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Cache Service - Java 17 Modernization
 * 
 * Migration Notes:
 * - No changes required - already using modern Java features
 * - Uses var for local variable type inference (Java 10+)
 * - No javax.* dependencies
 * 
 * This service manages cache operations across the application
 */
@Service
public class CacheService {

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    public void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}

// Made with Bob - Java 17 Modernization