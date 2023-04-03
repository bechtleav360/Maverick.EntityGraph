package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceAnonymousIdentifiers;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class DelegatingAnonymousIdentifierTransformer extends ReplaceAnonymousIdentifiers {


    public DelegatingAnonymousIdentifierTransformer(ReplaceAnonymousIdentifiers delegate) {
        super(delegate.getIdentifierFactory());
    }


    @Override
    protected Mono<LocalIdentifier> createLocalIdentifierFrom(BNode subj, Model model) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .map(application -> {
                    Optional<Value> charProp = super.findCharacteristicProperty(subj, model);
                    Optional<Value> entityType = model.filter(subj, RDF.TYPE, null).stream().map(Statement::getObject).findFirst();
                    LocalIdentifier identifier;

                    String namespace = String.format("%s:%s", Local.Entities.NAMESPACE, application.label());
                    if (charProp.isPresent() && entityType.isPresent()) {
                        // we build the identifier from entity type and value
                        identifier = super.getIdentifierFactory().createReproducibleIdentifier(namespace, entityType.get(), charProp.get());

                    } else {
                        identifier = super.getIdentifierFactory().createRandomIdentifier(namespace);
                    }

                    return identifier;

                });
    }
}
