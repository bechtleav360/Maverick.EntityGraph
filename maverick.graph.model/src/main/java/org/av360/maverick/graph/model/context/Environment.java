package org.av360.maverick.graph.model.context;

import org.av360.maverick.graph.model.enums.RepositoryType;

public class Environment {

    private final SessionContext parent;
    private String name;

    private RepositoryType repositoryType;
    private String scope;

    public Environment(SessionContext parent) {
        this.parent = parent;
    }


    public RepositoryType getRepositoryType() {
        return repositoryType;
    }

    public SessionContext withRepositoryType(RepositoryType repositoryType) {
        this.repositoryType = repositoryType;
        return this.parent;
    }

    public SessionContext withRepositoryType(String label) {
        this.repositoryType = RepositoryType.valueOf(label.toUpperCase());
        return this.parent;
    }


    public SessionContext setScope(String identifier) {
        this.scope = identifier;
        return this.parent;
    }

    public String getScope() {
        return scope;
    }
}
