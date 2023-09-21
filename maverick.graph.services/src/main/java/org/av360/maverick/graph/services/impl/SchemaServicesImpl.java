package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.errors.requests.UnknownPrefix;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.model.vocabulary.*;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.store.PersistedSchemaGraph;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j(topic = "graph.srvc.schema")
public class SchemaServicesImpl implements SchemaServices {
    private final PersistedSchemaGraph schemaStore;

    public SchemaServicesImpl(PersistedSchemaGraph schemaStore) {
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
    public Mono<Namespace> getNamespaceFor(String prefix) {
        return this.schemaStore.getNamespaceForPrefix(prefix).map(Mono::just).orElse(Mono.error(new UnknownPrefix(prefix)));
    }

    @Override
    public boolean isIndividualType(IRI iri) {
        boolean res =
                SDO.getIndividualTypes().contains(iri)
                        || SCHEMA.getIndividualTypes().contains(iri)
                        || RDFS.getIndividualTypes().contains(iri)
                        || DC.getIndividualTypes().contains(iri)
                        || DCTERMS.getIndividualTypes().contains(iri)
                        || SKOS.getIndividualTypes().contains(iri)
                        || ICAL.getIndividualTypes().contains(iri)
                        || ESCO.getIndividualTypes().contains(iri)
                        || FOAF.getIndividualTypes().contains(iri)
                ;
        return res;

    }

    @Override
    public boolean isClassifierType(IRI iri) {
        return
                SDO.getClassifierTypes().contains(iri)
                        || SCHEMA.getClassifierTypes().contains(iri)
                        || RDFS.getClassifierTypes().contains(iri)
                        || DC.getClassifierTypes().contains(iri)
                        || DCTERMS.getClassifierTypes().contains(iri)
                        || SKOS.getClassifierTypes().contains(iri)
                        || SKOSXL.getClassifierTypes().contains(iri)
                        || ICAL.getClassifierTypes().contains(iri)
                        || ESCO.getClassifierTypes().contains(iri)
                        || FOAF.getClassifierTypes().contains(iri)
                ;
    }

    @Override
    public boolean isCharacteristicProperty(IRI iri) {
        return
                SDO.getCharacteristicProperties().contains(iri)
                        || SCHEMA.getCharacteristicProperties().contains(iri)
                        || RDFS.getCharacteristicProperties().contains(iri)
                        || DC.getCharacteristicProperties().contains(iri)
                        || DCTERMS.getCharacteristicProperties().contains(iri)
                        || SKOS.getCharacteristicProperties().contains(iri)
                        || ICAL.getCharacteristicProperties().contains(iri)
                        || ESCO.getCharacteristicProperties().contains(iri)
                        || FOAF.getCharacteristicProperties().contains(iri)
        ;
    }

    protected String[] splitPrefixedIdentifier(String prefixedKey) {
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and label from path parameter " + prefixedKey);
        return property;
    }
}
