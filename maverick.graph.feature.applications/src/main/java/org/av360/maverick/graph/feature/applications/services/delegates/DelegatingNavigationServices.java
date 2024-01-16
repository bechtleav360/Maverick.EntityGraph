package org.av360.maverick.graph.feature.applications.services.delegates;

import org.av360.maverick.graph.feature.applications.model.domain.Application;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.NavigationServices;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelegatingNavigationServices implements NavigationServices {
    public static final String CONFIG_KEY_CUSTOM_LIST_QUERY = "custom_list_query";
    private final NavigationServices delegate;
    private final SimpleValueFactory vf;
    private ApplicationsService applicationsService;

    public DelegatingNavigationServices(NavigationServices delegate, ApplicationsService applicationsService) {
        this.applicationsService = applicationsService;
        this.delegate = delegate;
        this.vf = SimpleValueFactory.getInstance();
        ConfigurationKeysRegistry.add(CONFIG_KEY_CUSTOM_LIST_QUERY, "Custom query for listing entities within this application for navigation.");
    }

    @Override
    public Flux<AnnotatedStatement> start(SessionContext ctx) {

        return delegate.start(ctx).mergeWith(
                applicationsService.listApplications(Set.of(), ctx)
                        .collectList()
                        .map(list -> {
                            list.add(Application.DEFAULT);
                            return list;
                        })
                        .switchIfEmpty(Mono.just(List.of(Application.DEFAULT)))
                        .flatMapMany(applications -> Flux.create(sink -> {
                            Resource appsCollection = Values.bnode();
                            ModelBuilder builder = new ModelBuilder()
                                    .namedGraph(NavigationServices.NAVIGATION_CONTEXT)
                                    .subject(appsCollection)
                                    .add(RDF.TYPE, HYDRA.COLLECTION)
                                    .add(HYDRA.TITLE, "Scopes")
                                    .add(HYDRA.DESCRIPTION, "Collection of available scopes")
                                    .add(HYDRA.TOTAL_ITEMS, applications.size());

                            applications.forEach(application -> {
                                Resource appNode = Values.bnode(application.label());
                                builder.subject(appNode)
                                        .add(HYDRA.TITLE, application.label())
                                        .add(HYDRA.ENTRYPOINT, "?/api/s/%s/entities".formatted(application.label()))
                                        .add(HYDRA.VIEW, "?/nav/s/%s/entities".formatted(application.label()));


                                builder.add(appsCollection, HYDRA.MEMBER, appNode);

                            /*    IRI app = vf.createIRI(Local.Applications.NAMESPACE, application.key());
                                builder.setNamespace(application.label(), ResolvableUrlPrefix + "/api/s/" + application.label() + "/");
                                // FIXME: we only add the new namespace to late statements, later we just use the namespaces of the first statement

                                builder.subject(app)
                                        .add(ApplicationTerms.HAS_KEY, application.key())
                                        .add(ApplicationTerms.HAS_LABEL, application.label())
                                        .add(HYDRA.ENTRYPOINT, "/api/s/%s/entities".formatted(application.label()));*/
                            });

                            Set<Namespace> namespaces = builder.build().getNamespaces();
                            builder.build().stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces)).forEach(sink::next);
                            sink.complete();
                        })));
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<AnnotatedStatement> list(Map<String, String> requestParams, SessionContext ctx, @Nullable String query) {
        if(ctx.getEnvironment().hasScope()) {
            requestParams.put("scope", ctx.getEnvironment().getScope().label());
        }

        return delegate.list(requestParams, ctx, null);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<AnnotatedStatement> view(Map<String, String> requestParams, SessionContext ctx) {
        return this.delegate.view(requestParams, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<AnnotatedStatement> browse(Map<String, String> params, SessionContext ctx) {
        return delegate.browse(params, ctx);
    }

    @Override
    public Literal generateResolvableIRI(String path, Map<String, String> params) {
        return delegate.generateResolvableIRI(path, params);
    }

}
