package com.example.drifter.resource.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.DescribeClusterRequest;
import software.amazon.awssdk.services.eks.model.DescribeClusterResponse;

public class KubernetesClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(KubernetesClientFactory.class);

    public static ApiClient createClient(EksClient eksClient, String clusterName, String roleArn) {
        try {
            // Get cluster information
            DescribeClusterRequest request = DescribeClusterRequest.builder()
                    .name(clusterName)
                    .build();

            DescribeClusterResponse response = eksClient.describeCluster(request);
            String endpoint = response.cluster().endpoint();

            // For production use, you would need to implement proper EKS authentication
            // This is a simplified version that assumes kubectl is configured
            // In a real implementation, you'd use AWS IAM authenticator for Kubernetes

            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            logger.info("Created Kubernetes client for cluster: {}", clusterName);
            return client;

        } catch (Exception e) {
            logger.error("Failed to create Kubernetes client for cluster {}: {}", clusterName, e.getMessage());
            throw new RuntimeException("Failed to create Kubernetes client", e);
        }
    }
}
