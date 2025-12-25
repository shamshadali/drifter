package com.example.drifter.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ResourceConfig {
    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("tags")
    private Map<String, List<String>> tags;

    @JsonProperty("exclude")
    private List<String> exclude;

    @JsonProperty("include")
    private List<String> include;

    public ResourceConfig() {}

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, List<String>> getTags() {
        return tags;
    }

    public void setTags(Map<String, List<String>> tags) {
        this.tags = tags;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }
}
