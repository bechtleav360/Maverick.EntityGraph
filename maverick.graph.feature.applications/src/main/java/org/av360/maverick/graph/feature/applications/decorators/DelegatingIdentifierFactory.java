package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.springframework.util.StringUtils;

import java.io.Serializable;

public class DelegatingIdentifierFactory implements IdentifierFactory {

    private final IdentifierFactory delegate;

    private String scope;
    public DelegatingIdentifierFactory(IdentifierFactory identifierFactory) {
        this.delegate = identifierFactory;
    }


    public IdentifierFactory inScope(String scope) {
        this.scope = scope;
        return this;
    }


    @Override
    public LocalIdentifier createRandomIdentifier(String namespace) {
        if(StringUtils.hasLength(this.scope)) namespace += this.scope+":";

        return delegate.createRandomIdentifier(namespace);
    }

    @Override
    public LocalIdentifier createReproducibleIdentifier(String namespace, Serializable... parts) {
        if(StringUtils.hasLength(this.scope)) namespace += this.scope+":";

        return delegate.createReproducibleIdentifier(namespace, parts);
    }


}
