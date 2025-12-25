package com.example.drifter;

import com.example.drifter.config.ConfigurationReader;
import com.example.drifter.config.DrifterConfig;
import com.example.drifter.config.Environment;
import com.example.drifter.resource.Resource;
import com.example.drifter.resource.ResourceFinder;
import com.example.drifter.resource.k8s.DeploymentFinder;
import com.example.drifter.resource.k8s.KubernetesClientFactory;
import com.example.drifter.resource.lambda.LambdaFinder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DrifterApplication {
    private static final Logger logger = LoggerFactory.getLogger(DrifterApplication.class);

    private static final String DEFAULT_CONFIG_PATH = "./config.json";
    private static final String DEFAULT_FORMAT = "table";

    public static void main(String[] args) {
        Options options = createCommandLineOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("drifter", options);
                return;
            }

            String configPath = cmd.getOptionValue("config", DEFAULT_CONFIG_PATH);
            String format = cmd.getOptionValue("format", DEFAULT_FORMAT);
            boolean verbose = cmd.hasOption("verbose");

            if (verbose) {
                System.setProperty("logging.level.com.example.drifter", "DEBUG");
            }

            DrifterApplication app = new DrifterApplication();
            app.run(configPath, format);

        } catch (ParseException e) {
            logger.error("Failed to parse command line arguments: {}", e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("drifter", options);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Application failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(Option.builder("c")
                .longOpt("config")
                .desc("Config path in JSON or YAML format, defaults to ./config.json")
                .hasArg()
                .argName("FILE")
                .build());

        options.addOption(Option.builder("f")
                .longOpt("format")
                .desc("Output format: 'table' or 'json'")
                .hasArg()
                .argName("FORMAT")
                .build());

        options.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("Enable verbose output")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show this help message")
                .build());

        return options;
    }

    public void run(String configPath, String format) throws IOException, ExecutionException, InterruptedException {
        logger.info("Starting Drifter application");

        // Read configuration
        ConfigurationReader configReader = new ConfigurationReader();
        DrifterConfig config = configReader.readFile(configPath);

        logger.info("Loaded configuration with {} environments", config.getEnvironments().size());

        // Collect resources from all environments
        Map<String, List<Resource>> environmentResources = new HashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Environment env : config.getEnvironments()) {
            CompletableFuture<Void> future = processEnvironment(env, config, environmentResources);
            futures.add(future);
        }

        // Wait for all environments to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // Output results
        if ("json".equalsIgnoreCase(format)) {
            outputJson(environmentResources);
        } else {
            outputTable(config.getEnvironments(), environmentResources);
        }

        logger.info("Drifter application completed");
    }

    private CompletableFuture<Void> processEnvironment(Environment env, DrifterConfig config,
                                                      Map<String, List<Resource>> environmentResources) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Processing environment: {}", env.getName());

            try {
                // Create AWS credentials provider for this environment
                AwsCredentialsProvider credentialsProvider = createCredentialsProvider(env);

                List<CompletableFuture<List<Resource>>> resourceFutures = new ArrayList<>();

                // Fetch Lambda functions
                if (config.getLambdas() != null) {
                    LambdaClient lambdaClient = LambdaClient.builder()
                            .region(Region.of(env.getRegion()))
                            .credentialsProvider(credentialsProvider)
                            .build();

                    LambdaFinder lambdaFinder = new LambdaFinder(lambdaClient);
                    resourceFutures.add(lambdaFinder.findResources(config.getLambdas()));
                }

                // Fetch Kubernetes deployments
                if (config.getDeployments() != null) {
                    EksClient eksClient = EksClient.builder()
                            .region(Region.of(env.getRegion()))
                            .credentialsProvider(credentialsProvider)
                            .build();

                    ApiClient k8sClient = KubernetesClientFactory.createClient(eksClient, env.getClusterName(), env.getRole());
                    DeploymentFinder deploymentFinder = new DeploymentFinder(k8sClient);
                    resourceFutures.add(deploymentFinder.findResources(config.getDeployments()));
                }

                // Wait for all resource types to complete
                List<Resource> allResources = new ArrayList<>();
                for (CompletableFuture<List<Resource>> future : resourceFutures) {
                    allResources.addAll(future.get());
                }

                synchronized (environmentResources) {
                    environmentResources.put(env.getName(), allResources);
                }

                logger.info("Completed processing environment: {} with {} resources", env.getName(), allResources.size());

            } catch (Exception e) {
                logger.error("Failed to process environment {}: {}", env.getName(), e.getMessage(), e);
            }
        });
    }

    private AwsCredentialsProvider createCredentialsProvider(Environment env) {
        // For simplicity in this migration, using DefaultCredentialsProvider
        // In production, you would implement proper role assumption
        // using AssumeRoleCredentialsProvider with appropriate dependencies
        if (env.getRole() != null && !env.getRole().isEmpty()) {
            logger.info("Role specified: {} for environment: {} (using default credentials)",
                    env.getRole(), env.getName());
        }

        return DefaultCredentialsProvider.create();
    }

    private void outputJson(Map<String, List<Resource>> environmentResources) throws IOException {
        List<Map<String, Object>> output = new ArrayList<>();
        Map<String, Set<String>> resourceVersions = new HashMap<>();

        // Organize data by resource name
        for (Map.Entry<String, List<Resource>> entry : environmentResources.entrySet()) {
            String envName = entry.getKey();
            for (Resource resource : entry.getValue()) {
                resourceVersions.computeIfAbsent(resource.getName(), k -> new HashSet<>()).add(envName);
            }
        }

        // Build JSON output
        for (Map.Entry<String, List<Resource>> entry : environmentResources.entrySet()) {
            String envName = entry.getKey();
            for (Resource resource : entry.getValue()) {
                Map<String, Object> resourceMap = findOrCreateResourceMap(output, resource.getName(), resource.getType());
                Map<String, String> versions = (Map<String, String>) resourceMap.get("versions");
                versions.put(envName, resource.getVersion());
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
        System.out.println(json);
    }

    private Map<String, Object> findOrCreateResourceMap(List<Map<String, Object>> output, String name, String type) {
        for (Map<String, Object> resourceMap : output) {
            if (name.equals(resourceMap.get("name"))) {
                return resourceMap;
            }
        }

        Map<String, Object> newResource = new HashMap<>();
        newResource.put("name", name);
        newResource.put("type", type);
        newResource.put("versions", new HashMap<String, String>());
        output.add(newResource);

        return newResource;
    }

    private void outputTable(List<Environment> environments, Map<String, List<Resource>> environmentResources) {
        System.out.printf("%-8s%-16s%-32s", "#", "TYPE", "NAME");

        for (Environment env : environments) {
            System.out.printf("%-32s", env.getName().toUpperCase());
        }
        System.out.println();

        // Organize resources by name
        Map<String, Map<String, Resource>> resourcesByName = new HashMap<>();
        for (Map.Entry<String, List<Resource>> entry : environmentResources.entrySet()) {
            String envName = entry.getKey();
            for (Resource resource : entry.getValue()) {
                resourcesByName.computeIfAbsent(resource.getName(), k -> new HashMap<>())
                        .put(envName, resource);
            }
        }

        int counter = 1;
        for (Map.Entry<String, Map<String, Resource>> entry : resourcesByName.entrySet()) {
            String resourceName = entry.getKey();
            Map<String, Resource> envVersions = entry.getValue();

            // Get the type from any environment
            String type = envVersions.values().iterator().next().getType();

            System.out.printf("%-8d%-16s%-32s", counter++, type, resourceName);

            // Detect drift
            Set<String> uniqueVersions = new HashSet<>();
            for (Environment env : environments) {
                Resource resource = envVersions.get(env.getName());
                String version = resource != null ? resource.getVersion() : "N/A";
                uniqueVersions.add(version);

                String driftIndicator = uniqueVersions.size() > 1 ? "❌ " : "✅ ️";
                System.out.printf("%s%-30s", driftIndicator, version);
            }
            System.out.println();
        }
    }
}
