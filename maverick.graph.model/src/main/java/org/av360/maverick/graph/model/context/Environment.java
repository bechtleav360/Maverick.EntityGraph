package org.av360.maverick.graph.model.context;

import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Environment implements Serializable {


    private final SessionContext parent;
    private String stage;
    private RepositoryType repositoryType;
    private Map<RepositoryConfig, Serializable> configuration;
    private Scope scope;
    private boolean[] flags = {false, false, false};

    public Environment(SessionContext parent) {
        this.configuration = new HashMap<>();
        this.parent = parent;
    }

    public SessionContext getSessionContext() {
        return this.parent;
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

    public void setScope(String identifier) {
        this.scope = new Scope(identifier, null);
    }

    public boolean hasConfiguration(RepositoryConfig key) {
        return this.configuration.containsKey(key);
    }

    public Optional<String> getConfiguration(RepositoryConfig key) {
        return Optional.ofNullable(this.configuration.get(key)).map(Object::toString);
    }

    public Environment setConfiguration(RepositoryConfig key, Serializable value) {
        this.configuration.put(key, value);
        return this;
    }

    public Environment setFlag(RepositoryFlag flag, boolean value) {
        switch (flag) {
            case PUBLIC -> this.flags[0] = value;
            case PERSISTENT -> this.flags[1] = value;
            case REMOTE -> this.flags[2] = value;
        }
        return this;
    }

    public boolean isFlagged(RepositoryFlag flag) {
        return switch (flag) {
            case PUBLIC -> this.flags[0];
            case PERSISTENT -> this.flags[1];
            case REMOTE -> this.flags[2];
        };
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

    public enum RepositoryConfig {
        IDENTIFIER,
        LABEL,
        KEY,
        PATH,

    }


    public enum RepositoryFlag {
        PUBLIC,
        PERSISTENT,
        REMOTE
    }
}
