package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.rdf.models.TripleModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import reactor.core.publisher.Mono;

import java.util.Map;

public class DelegatingExternalIdentifierTransformer extends ReplaceExternalIdentifiers {


    public DelegatingExternalIdentifierTransformer(ReplaceExternalIdentifiers delegate) {
        super(delegate.getIdentifierFactory());
    }

    @Override
    public Mono<? extends TripleModel> handle(TripleModel model, Map<String, String> parameters) {
        return super.handle(model, parameters);
    }

    @Override
    protected Mono<LocalIdentifier> createLocalIdentifierFrom(IRI iri, Model model) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .map(application -> {
                    String namespace = String.format("%s:%s", Local.Entities.NAMESPACE, application.label());
                    return super.getIdentifierFactory().createReproducibleIdentifier(namespace, iri);
                });
    }
}
