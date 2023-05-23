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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.WebSession;
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

    public Flux<AnnotatedStatement> start(Authentication authentication, WebSession session) {


        return delegate.start(authentication, session).mergeWith(
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
                                builder.setNamespace(application.label(), ResolvableUrlPrefix+"/api/s/"+application.label()+"/");
                                // FIXME: we only add the new namespace to late statements, later we just use the namespaces of the first statement

                                builder.subject(app)
                                        .add(ApplicationTerms.HAS_KEY, application.key())
                                        .add(ApplicationTerms.HAS_LABEL, application.label())
                                        .add(HYDRA.ENTRYPOINT, String.format(ResolvableUrlPrefix+"?/api/s/%s/entities",  application.label()))
                                        .add(appsCollection, HYDRA.MEMBER, app);
                            });

                            Set<Namespace> namespaces = builder.build().getNamespaces();
                            builder.build().stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces)).forEach(sink::next);
                            sink.complete();


                        })));
    }

    @Override
    public Flux<AnnotatedStatement> browse(MultiValueMap<String, String> params, Authentication authentication, WebSession session) {
        return delegate.browse(params, authentication, session);
    }
}
