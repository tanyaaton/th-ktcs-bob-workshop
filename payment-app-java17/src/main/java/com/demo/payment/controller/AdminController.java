package com.demo.payment.controller;

import com.demo.payment.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Controller - Java 17 Modernization
 * 
 * Migration Notes:
 * - No javax.* dependencies to migrate
 * - Could use Java 17 text blocks for multi-line responses (if needed)
 * - Could use Map.of() for immutable maps (Java 9+)
 * 
 * Minimal changes required - controller logic is already clean
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final CacheService cacheService;

    public AdminController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        try {
            cacheService.clearAllCaches();
            
            // Java 9+ Map.of() for immutable maps (more concise)
            // But using HashMap for consistency with error response
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All caches cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to clear cache: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

// Made with Bob - Java 17 Modernization