package com.example.drifter.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DrifterConfig {
    @JsonProperty("envs")
    private List<Environment> environments;

    @JsonProperty("lambdas")
    private ResourceConfig lambdas;

    @JsonProperty("deployments")
    private ResourceConfig deployments;

    public DrifterConfig() {}

    public List<Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<Environment> environments) {
        this.environments = environments;
    }

    public ResourceConfig getLambdas() {
        return lambdas;
    }

    public void setLambdas(ResourceConfig lambdas) {
        this.lambdas = lambdas;
    }

    public ResourceConfig getDeployments() {
        return deployments;
    }

    public void setDeployments(ResourceConfig deployments) {
        this.deployments = deployments;
    }
}
