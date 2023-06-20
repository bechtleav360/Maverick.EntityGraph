package org.av360.maverick.graph.model.context;

import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Environment implements  Serializable{



    public SessionContext getSessionContext() {
        return this.parent;
    }

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
    private Scope scope;

    public Environment(SessionContext parent) {
        this.configuration = new HashMap<>();
        this.parent = parent;
    }



    public RepositoryType getRepositoryType() {
        return repositoryType;
    }

    public Environment setRepositoryType(RepositoryType repositoryType) {
        this.repositoryType = repositoryType;
        return this;
    }

    public Environment setRepositoryType(String label) {
        this.repositoryType = RepositoryType.valueOf(label.toUpperCase());
        return this;
    }


    public SessionContext withRepositoryType(RepositoryType repositoryType) {
        this.repositoryType = repositoryType;
        return this.parent;
    }


    public void setScope(String identifier) {
        this.scope = new Scope(identifier, null);
    }

    public Environment withScope(String identifier) {
        return this.withScope(identifier, null);
    }

    public Environment withScope(String identifier, @Nullable Object details) {
        this.scope = new Scope(identifier, details);
        return this;
    }


    public boolean hasScope() {
        return Objects.nonNull(this.scope);
    }

    public Scope getScope() {
        return Objects.isNull(this.scope) ? new Scope("default", null) : this.scope;
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



    public boolean isAuthorized() {
        return Objects.nonNull(this.parent) && Objects.nonNull(this.parent.getDecision()) && this.parent.getDecision().isGranted();
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
                Objects.nonNull(this.scope) ? this.scope.label() : "default",
                Objects.isNull(this.repositoryType) ? "?" : this.repositoryType.toString(),
                StringUtils.hasLength(this.stage) ? this.stage : "?"
        );
    }
}
