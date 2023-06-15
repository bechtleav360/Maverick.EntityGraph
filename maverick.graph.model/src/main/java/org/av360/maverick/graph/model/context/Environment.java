package org.av360.maverick.graph.model.context;

import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Environment implements  Serializable{




    public enum RepositoryConfigurationKey {
        IDENTIFIER,
        LABEL,
        KEY,
        FLAG_PUBLIC,
        FLAG_PERSISTENT
    }


    private final SessionContext parent;
    private String stage;

    private RepositoryType repositoryType;

    private Map<RepositoryConfigurationKey, Serializable>  configuration;
    private String scope;

    public Environment(SessionContext parent) {
        this.configuration = new HashMap<>();
        this.parent = parent;
    }


    public RepositoryType getRepositoryType() {
        return repositoryType;
    }

    public SessionContext setRepositoryType(RepositoryType repositoryType) {
        this.repositoryType = repositoryType;
        return this.parent;
    }

    public SessionContext setRepositoryType(String label) {
        this.repositoryType = RepositoryType.valueOf(label.toUpperCase());
        return this.parent;
    }


    public SessionContext setScope(String identifier) {
        this.scope = identifier;
        return this.parent;
    }

    public boolean hasScope() {
        return StringUtils.hasLength(this.getScope());
    }

    public String getScope() {
        return scope;
    }

    public boolean hasConfiguration(RepositoryConfigurationKey key) {
        return this.configuration.containsKey(key);
    }

    public Optional<String> getConfiguration(RepositoryConfigurationKey key) {
        return Optional.ofNullable(this.configuration.get(key)).map(Object::toString);
    }

    public SessionContext setConfiguration(RepositoryConfigurationKey key, Serializable value) {
        this.configuration.put(key, value);
        return this.parent;
    }


    public String getStage() {
        return stage;
    }

    public SessionContext setStage(String stage) {
        this.stage = stage;
        return this.parent;
    }


    @Override
    public String toString() {
        return "%s:%s:%s".formatted(
                StringUtils.hasLength(this.scope) ? this.scope : "default",
                Objects.isNull(this.repositoryType) ? "?" : this.repositoryType.toString(),
                StringUtils.hasLength(this.stage) ? this.stage : "?"
        );
    }
}
