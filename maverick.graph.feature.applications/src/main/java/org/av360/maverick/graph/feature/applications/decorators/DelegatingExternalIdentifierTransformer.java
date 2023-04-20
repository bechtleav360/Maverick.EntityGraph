package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.transformers.replaceIdentifiers.ReplaceExternalIdentifiers;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
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
    protected Mono<IRI> createLocalIdentifierFrom(IRI iri, Model model) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .map(application -> {

                    LocalIdentifier identifier = super.getIdentifierFactory().createReproducibleIdentifier(Local.Entities.NAMESPACE, iri);
                    String scopedIdentifier = String.format("%s.%s", application.label(), identifier.getLocalName());

                    return valueFactory.createIRI(Local.Entities.NAMESPACE, scopedIdentifier);
                });
    }
}
