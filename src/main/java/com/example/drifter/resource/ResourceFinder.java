package com.example.drifter.resource;

import com.example.drifter.config.ResourceConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public interface ResourceFinder {
    CompletableFuture<List<Resource>> findResources(ResourceConfig config);

    default boolean isExcluded(String name, List<String> excludePatterns) {
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }

        return excludePatterns.stream()
                .map(Pattern::compile)
                .anyMatch(pattern -> pattern.matcher(name).matches());
    }
}
