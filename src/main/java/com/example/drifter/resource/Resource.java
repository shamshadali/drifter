package com.example.drifter.resource;

import java.util.Objects;

public class Resource {
    private String type;
    private String name;
    private String version;

    public Resource() {}

    public Resource(String type, String name, String version) {
        this.type = type;
        this.name = name;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(type, resource.type) &&
               Objects.equals(name, resource.name) &&
               Objects.equals(version, resource.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, version);
    }

    @Override
    public String toString() {
        return "Resource{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
