package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Mono;

public class DelegatingIdentifierServices implements IdentifierServices{

    private final IdentifierServices delegate;
    private final ApplicationsService applicationsService;

    public DelegatingIdentifierServices(IdentifierServices delegate, ApplicationsService applicationsService) {
        this.delegate = delegate;
        this.applicationsService = applicationsService;
    }


    @Override
    public Mono<String> validate(String identifier) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .map(application -> String.format("%s.%s", application.label(), identifier))
                .switchIfEmpty(delegate.validate(identifier));

    }

    @Override
    public Mono<IRI> asIRI(String key) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .map(application -> String.format("%s.%s", application.label(), key))
                .flatMap(delegate::asIRI);
    }
}
