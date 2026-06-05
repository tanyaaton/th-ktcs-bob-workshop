package com.demo.payment.controller;

import com.demo.payment.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "All caches cleared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to clear cache: " + e.getMessage()
            ));
        }
    }
}

// Modernized with Java 17

// Made with Bob
