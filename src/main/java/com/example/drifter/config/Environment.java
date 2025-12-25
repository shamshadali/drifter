package com.example.drifter.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Environment {
    @JsonProperty("name")
    private String name;

    @JsonProperty("region")
    private String region;

    @JsonProperty("role")
    private String role;

    @JsonProperty("clusterName")
    private String clusterName;

    public Environment() {}

    public Environment(String name, String region, String role, String clusterName) {
        this.name = name;
        this.region = region;
        this.role = role;
        this.clusterName = clusterName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
