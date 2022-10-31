package io.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.base.AbstractNamespace;

public class EntityNamespace extends AbstractNamespace {

    private final String prefix;
    private final String name;

    private EntityNamespace(String prefix, String name) {
        this.prefix = prefix;
        this.name = name;
    }

    public static org.eclipse.rdf4j.model.Namespace of(String prefix, String name) {
        return new EntityNamespace(prefix, name);
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
