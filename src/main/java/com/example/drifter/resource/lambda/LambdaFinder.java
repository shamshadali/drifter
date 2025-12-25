package com.example.drifter.resource.lambda;

import com.example.drifter.config.ResourceConfig;
import com.example.drifter.resource.Resource;
import com.example.drifter.resource.ResourceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LambdaFinder implements ResourceFinder {
    private static final Logger logger = LoggerFactory.getLogger(LambdaFinder.class);

    private final LambdaClient lambdaClient;

    public LambdaFinder(LambdaClient lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    @Override
    public CompletableFuture<List<Resource>> findResources(ResourceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            if (config.getInclude() != null && !config.getInclude().isEmpty()) {
                return findByName(config.getInclude(), config.getExclude());
            }

            // If no include list, we would need to implement tag-based search
            // For now, return empty list as the original Go code doesn't have a clear implementation
            logger.warn("Tag-based Lambda discovery not implemented yet");
            return new ArrayList<>();
        });
    }

    private List<Resource> findByName(List<String> functionNames, List<String> excludePatterns) {
        List<Resource> resources = new ArrayList<>();

        for (String functionName : functionNames) {
            if (isExcluded(functionName, excludePatterns)) {
                logger.debug("Excluding Lambda function: {}", functionName);
                continue;
            }

            try {
                GetFunctionRequest request = GetFunctionRequest.builder()
                        .functionName(functionName)
                        .build();

                GetFunctionResponse response = lambdaClient.getFunction(request);

                String version = extractVersion(response.tags());

                resources.add(new Resource("Lambda", functionName, version));
                logger.debug("Found Lambda function: {} with version: {}", functionName, version);

            } catch (ResourceNotFoundException e) {
                logger.warn("Lambda function not found: {}", functionName);
            } catch (SdkException e) {
                logger.error("Error retrieving Lambda function {}: {}", functionName, e.getMessage());
            }
        }

        return resources;
    }

    private String extractVersion(Map<String, String> tags) {
        if (tags == null) {
            return "N/A";
        }

        String version = tags.get("version");
        return (version != null && !version.isEmpty()) ? version : "N/A";
    }
}
