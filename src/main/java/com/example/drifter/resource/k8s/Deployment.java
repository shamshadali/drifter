package com.example.drifter.resource.k8s;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Deployment {
    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    public Deployment() {}

    public Deployment(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deployment that = (Deployment) o;
        return Objects.equals(name, that.name) && Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace);
    }

    @Override
    public String toString() {
        return "Deployment{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
