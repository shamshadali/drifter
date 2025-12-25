package com.example.drifter.resource.lambda;

import com.example.drifter.config.ResourceConfig;
import com.example.drifter.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LambdaFinderTest {

    @Mock
    private LambdaClient lambdaClient;

    private LambdaFinder lambdaFinder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lambdaFinder = new LambdaFinder(lambdaClient);
    }

    @Test
    void testFindResourcesWithIncludeList() throws Exception {
        // Arrange
        ResourceConfig config = new ResourceConfig();
        config.setInclude(List.of("test-lambda"));

        GetFunctionResponse response = GetFunctionResponse.builder()
                .tags(Map.of("version", "1.0.0"))
                .build();

        when(lambdaClient.getFunction(any(GetFunctionRequest.class)))
                .thenReturn(response);

        // Act
        CompletableFuture<List<Resource>> future = lambdaFinder.findResources(config);
        List<Resource> resources = future.get();

        // Assert
        assertEquals(1, resources.size());
        Resource resource = resources.get(0);
        assertEquals("Lambda", resource.getType());
        assertEquals("test-lambda", resource.getName());
        assertEquals("1.0.0", resource.getVersion());
    }

    @Test
    void testFindResourcesWithMissingVersion() throws Exception {
        // Arrange
        ResourceConfig config = new ResourceConfig();
        config.setInclude(List.of("test-lambda"));

        GetFunctionResponse response = GetFunctionResponse.builder()
                .tags(Map.of())
                .build();

        when(lambdaClient.getFunction(any(GetFunctionRequest.class)))
                .thenReturn(response);

        // Act
        CompletableFuture<List<Resource>> future = lambdaFinder.findResources(config);
        List<Resource> resources = future.get();

        // Assert
        assertEquals(1, resources.size());
        assertEquals("N/A", resources.get(0).getVersion());
    }

    @Test
    void testFindResourcesWithNotFoundException() throws Exception {
        // Arrange
        ResourceConfig config = new ResourceConfig();
        config.setInclude(List.of("non-existent-lambda"));

        when(lambdaClient.getFunction(any(GetFunctionRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        // Act
        CompletableFuture<List<Resource>> future = lambdaFinder.findResources(config);
        List<Resource> resources = future.get();

        // Assert
        assertEquals(0, resources.size());
    }
}
