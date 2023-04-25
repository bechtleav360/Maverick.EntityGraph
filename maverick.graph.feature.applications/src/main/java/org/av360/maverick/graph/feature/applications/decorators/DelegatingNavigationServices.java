package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.vocab.ApplicationTerms;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.GuestToken;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.NavigationServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
public class DelegatingNavigationServices implements NavigationServices {
    private final NavigationServices delegate;
    private final ApplicationsService applicationsService;
    private final SimpleValueFactory vf;

    public DelegatingNavigationServices(NavigationServices delegate, ApplicationsService applicationsService) {
        this.delegate = delegate;
        this.applicationsService = applicationsService;
        this.vf = SimpleValueFactory.getInstance();
    }

    public Flux<AnnotatedStatement> start() {


        return delegate.start().mergeWith(
                ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                        .switchIfEmpty(Mono.just(new GuestToken()))
                        .flatMapMany(applicationsService::listApplications)
                        .collectList()
                        .flatMapMany(applications -> Flux.create(sink -> {
                            IRI appsCollection = vf.createIRI(Local.URN_PREFIX, "ApplicationSet");
                            IRI start = vf.createIRI(Local.NAMESPACE, "Start");
                            ModelBuilder builder = new ModelBuilder()
                                    .subject(appsCollection)
                                    .add(RDF.TYPE, HYDRA.COLLECTION)
                                    .add(HYDRA.TOTAL_ITEMS, applications.size())
                                    .subject(start)
                                    .add(vf.createIRI(Local.NAMESPACE, "applications"), appsCollection);

                            applications.forEach(application -> {
                                IRI app = vf.createIRI(Local.Applications.NAMESPACE, application.key());
                                builder.subject(app)
                                        .add(ApplicationTerms.HAS_KEY, application.key())
                                        .add(ApplicationTerms.HAS_LABEL, application.label())
                                        .add(HYDRA.ENTRYPOINT, String.format("?/api/s/%s/entities",  application.label()))
                                        .add(appsCollection, HYDRA.MEMBER, app);
                            });

                            Set<Namespace> namespaces = builder.build().getNamespaces();
                            builder.build().stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces)).forEach(sink::next);
                            sink.complete();


                        })));
    }
}
