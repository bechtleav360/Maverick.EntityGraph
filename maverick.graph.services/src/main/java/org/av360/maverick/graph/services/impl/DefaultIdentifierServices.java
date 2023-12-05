package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Map;

@Service
@Slf4j
public class DefaultIdentifierServices implements IdentifierServices {

    public DefaultIdentifierServices() {

    }

    @Override
    public String validate(String identifier, Environment environment) {
        return identifier;
    }

    @Override
    public IRI validateIRI(IRI identifier, Environment environment, @Nullable String endpoint, @Nullable Map<String, String> headers) {
        return Values.iri(environment.getRepositoryType().getIdentifierNamespace(), identifier.getLocalName());
    }


}
