package com.example.drifter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationReader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationReader.class);

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public ConfigurationReader() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public DrifterConfig readFile(String configPath) throws IOException {
        Path path = Paths.get(configPath);

        if (!Files.exists(path)) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        String content = Files.readString(path);
        String extension = getFileExtension(configPath);

        ObjectMapper mapper = getMapperForExtension(extension);

        try {
            return mapper.readValue(content, DrifterConfig.class);
        } catch (IOException e) {
            logger.error("Failed to parse configuration file: {}", configPath, e);
            throw new IOException("Failed to parse configuration file: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1).toLowerCase();
    }

    private ObjectMapper getMapperForExtension(String extension) {
        return switch (extension) {
            case "yaml", "yml" -> yamlMapper;
            case "json" -> jsonMapper;
            default -> {
                logger.warn("Unknown file extension '{}', defaulting to JSON parser", extension);
                yield jsonMapper;
            }
        };
    }
}
