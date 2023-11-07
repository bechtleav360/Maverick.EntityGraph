package org.av360.maverick.graph.feature.applications.services.delegates;

import org.av360.maverick.graph.feature.applications.model.domain.Application;
import org.av360.maverick.graph.feature.applications.model.vocab.ApplicationTerms;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.NavigationServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelegatingNavigationServices implements NavigationServices {
    private final NavigationServices delegate;
    private ApplicationsService applicationsService;
    private final SimpleValueFactory vf;

    public static final String CONFIG_KEY_CUSTOM_LIST_QUERY = "custom_list_query";

    public DelegatingNavigationServices(NavigationServices delegate, ApplicationsService applicationsService) {
        this.applicationsService = applicationsService;
        this.delegate = delegate;
        this.vf = SimpleValueFactory.getInstance();
        ConfigurationKeysRegistry.add(CONFIG_KEY_CUSTOM_LIST_QUERY, "Custom query for listing entities within this application for navigation.");
    }

    @Override
    public Flux<AnnotatedStatement> start(SessionContext ctx) {

        return delegate.start(ctx).mergeWith(
                applicationsService.listApplications(ctx)
                        .collectList()
                        .map(list -> {
                            list.add(Application.DEFAULT);
                            return list;
                        })
                        .switchIfEmpty(Mono.just(List.of(Application.DEFAULT)))
                        .flatMapMany(applications -> Flux.create(sink -> {
                            IRI appsCollection = vf.createIRI(ResolvableUrlPrefix+"/api/applications");
                            Resource root = vf.createIRI(Local.URN_PREFIX, "nav");

                            ModelBuilder builder = new ModelBuilder()
                                    .subject(appsCollection)
                                    .add(RDF.TYPE, HYDRA.COLLECTION)
                                    .add(HYDRA.TOTAL_ITEMS, applications.size())
                                    .subject(root)
                                    .add(HYDRA.ENTRYPOINT, appsCollection);

                            applications.forEach(application -> {
                                IRI app = vf.createIRI(Local.Applications.NAMESPACE, application.key());
                                builder.setNamespace(application.label(), ResolvableUrlPrefix + "/api/s/" + application.label() + "/");
                                // FIXME: we only add the new namespace to late statements, later we just use the namespaces of the first statement

                                builder.subject(app)
                                        .add(ApplicationTerms.HAS_KEY, application.key())
                                        .add(ApplicationTerms.HAS_LABEL, application.label())
                                        .add(HYDRA.ENTRYPOINT, "/api/s/%s/entities".formatted(application.label()))
                                        .add(appsCollection, HYDRA.MEMBER, app);
                            });

                            Set<Namespace> namespaces = builder.build().getNamespaces();
                            builder.build().stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces)).forEach(sink::next);
                            sink.complete();
                        })));
    }

    @Override
    public Flux<AnnotatedStatement> list(Map<String, String> requestParams, SessionContext ctx, @Nullable String query) {
        if(ctx.getEnvironment().hasScope()) {
            return this.applicationsService.getApplication(ctx.getEnvironment().getScope().label(), ctx)
                    .filter(application -> application.configuration().containsKey(CONFIG_KEY_CUSTOM_LIST_QUERY))
                    .map(application -> application.configuration().get(CONFIG_KEY_CUSTOM_LIST_QUERY))
                    .flatMapMany(appQuery ->  delegate.list(requestParams, ctx, appQuery.toString()))
                    .switchIfEmpty(delegate.list(requestParams, ctx, null));
        }
        else return delegate.list(requestParams, ctx, null);
    }

    @Override
    public Flux<AnnotatedStatement> browse(Map<String, String> params, SessionContext ctx) {
        return delegate.browse(params, ctx);
    }

    @Override
    public IRI generateResolvableIRI(String path, Map<String, String> params) {
        return delegate.generateResolvableIRI(path, params);
    }


    @Autowired
    public void setApplicationsService(ApplicationsService applicationsService) {
        this.applicationsService = applicationsService;
    }
}
