package com.example.drifter.resource.k8s;

import com.example.drifter.config.ResourceConfig;
import com.example.drifter.resource.Resource;
import com.example.drifter.resource.ResourceFinder;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeploymentFinder implements ResourceFinder {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentFinder.class);
    private static final Pattern IMAGE_VERSION_PATTERN = Pattern.compile(".+:(\\d+\\.\\d+\\.\\d+(-SNAPSHOT-.+)?)");

    private final ApiClient apiClient;
    private final AppsV1Api appsV1Api;

    public DeploymentFinder(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.appsV1Api = new AppsV1Api(apiClient);
    }

    @Override
    public CompletableFuture<List<Resource>> findResources(ResourceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (config.getInclude() != null && !config.getInclude().isEmpty()) {
                    return findByName(config.getInclude(), config.getNamespace(), config.getExclude());
                } else {
                    return findByTags(config.getTags(), config.getNamespace(), config.getExclude());
                }
            } catch (Exception e) {
                logger.error("Error finding deployments: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    private List<Resource> findByName(List<String> deploymentNames, String namespace, List<String> excludePatterns) throws ApiException {
        List<Resource> resources = new ArrayList<>();

        for (String deploymentName : deploymentNames) {
            if (isExcluded(deploymentName, excludePatterns)) {
                logger.debug("Excluding deployment: {}", deploymentName);
                continue;
            }

            try {
                V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                        deploymentName,
                        namespace != null ? namespace : "default",
                        null
                );

                String version = extractImageVersion(deployment, deploymentName);
                resources.add(new Resource("Deployment", deploymentName, version));
                logger.debug("Found deployment: {} with version: {}", deploymentName, version);

            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    logger.warn("Deployment not found: {} in namespace: {}", deploymentName, namespace);
                } else {
                    logger.error("Error retrieving deployment {}: {}", deploymentName, e.getMessage());
                }
            }
        }

        return resources;
    }

    private List<Resource> findByTags(java.util.Map<String, List<String>> tags, String namespace, List<String> excludePatterns) throws ApiException {
        List<Resource> resources = new ArrayList<>();

        if (tags == null || tags.isEmpty()) {
            return resources;
        }

        String labelSelector = buildLabelSelector(tags);

        try {
            V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(
                    namespace != null ? namespace : "default",
                    null, null, null, null, labelSelector, null, null, null, null, null
            );

            if (deploymentList.getItems() != null) {
                for (V1Deployment deployment : deploymentList.getItems()) {
                    String deploymentName = deployment.getMetadata().getName();

                    if (isExcluded(deploymentName, excludePatterns)) {
                        logger.debug("Excluding deployment: {}", deploymentName);
                        continue;
                    }

                    String version = extractImageVersion(deployment, deploymentName);
                    resources.add(new Resource("Deployment", deploymentName, version));
                    logger.debug("Found deployment: {} with version: {}", deploymentName, version);
                }
            }
        } catch (ApiException e) {
            logger.error("Error listing deployments with label selector {}: {}", labelSelector, e.getMessage());
            throw e;
        }

        return resources;
    }

    private String buildLabelSelector(java.util.Map<String, List<String>> tags) {
        List<String> selectors = new ArrayList<>();

        for (java.util.Map.Entry<String, List<String>> entry : tags.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (values.size() == 1) {
                selectors.add(key + "=" + values.get(0));
            } else {
                selectors.add(key + " in (" + String.join(",", values) + ")");
            }
        }

        return String.join(",", selectors);
    }

    private String extractImageVersion(V1Deployment deployment, String deploymentName) {
        if (deployment.getSpec() == null ||
            deployment.getSpec().getTemplate() == null ||
            deployment.getSpec().getTemplate().getSpec() == null ||
            deployment.getSpec().getTemplate().getSpec().getContainers() == null) {
            return "N/A";
        }

        for (V1Container container : deployment.getSpec().getTemplate().getSpec().getContainers()) {
            if (deploymentName.equals(container.getName()) && container.getImage() != null) {
                return extractVersionFromImage(container.getImage());
            }
        }

        // If no container matches deployment name, try the first container
        List<V1Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        if (!containers.isEmpty() && containers.get(0).getImage() != null) {
            return extractVersionFromImage(containers.get(0).getImage());
        }

        return "N/A";
    }

    private String extractVersionFromImage(String image) {
        Matcher matcher = IMAGE_VERSION_PATTERN.matcher(image);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "N/A";
    }
}
