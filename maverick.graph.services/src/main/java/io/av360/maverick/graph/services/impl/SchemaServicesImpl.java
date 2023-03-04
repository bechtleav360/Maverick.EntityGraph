package io.av360.maverick.graph.services.impl;

import io.av360.maverick.graph.model.errors.UnknownPrefix;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import io.av360.maverick.graph.services.SchemaServices;
import io.av360.maverick.graph.store.SchemaStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j(topic = "graph.srvc.schema")
public class SchemaServicesImpl implements SchemaServices {
    private final SchemaStore schemaStore;

    public SchemaServicesImpl(SchemaStore schemaStore) {
        this.schemaStore = schemaStore;
    }

    @Override
    public Mono<IRI> resolvePrefixedName(String prefixedName) {
        String[] property = splitPrefixedIdentifier(prefixedName);
        return this.getNamespaceFor(property[0])
                .map(namespace -> SimpleValueFactory.getInstance().createIRI(namespace.getName(), property[1]))
                .doOnError(err -> log.warn("Failed to resolve property {} due to error: '{}': ", prefixedName, err.getMessage()))
                .doOnSuccess(iri -> log.trace("Resolved property {} to qualified name '{}'", prefixedName, iri));
    }

    @Override
    public Mono<IRI> resolveLocalName(String name) {
        return Mono.just(LocalIRI.withDefaultNamespace(name));
    }

    @Override
    public Mono<Namespace> getNamespaceFor(String prefix) throws UnknownPrefix {
        return this.schemaStore.listNamespaces()
                .filter(namespace -> namespace.getPrefix().equalsIgnoreCase(prefix))
                .switchIfEmpty(Mono.error(new UnknownPrefix(prefix)))
                .single();
    }

    protected String[] splitPrefixedIdentifier(String prefixedKey) {
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and label from path parameter " + prefixedKey);
        return property;
    }
}
