package org.av360.maverick.graph.feature.applications.services;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.SessionContextBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ApplicationContextBuilder implements SessionContextBuilder {

    private final ApplicationsService applicationsService;

    public ApplicationContextBuilder(ApplicationsService applicationsService) {
        this.applicationsService = applicationsService;
    }

    @Override
    public Mono<SessionContext> build(SessionContext context) {
        if (context.getEnvironment().hasScope()) {
            return this.buildApplicationConfiguration(context);
        } else {
            return ReactiveApplicationContextHolder.getRequestedApplicationLabel()
                    .map(applicationLabel -> context.updateEnvironment(env -> env.setScope(applicationLabel)))
                    .flatMap(this::buildApplicationConfiguration)
                    .switchIfEmpty(Mono.just(context));

        }

    }

    private Mono<SessionContext> buildApplicationConfiguration(SessionContext context) {
        return this.applicationsService.getApplicationByLabel(context.getEnvironment().getScope().label(), SessionContext.SYSTEM)
                .map(application ->
                        context.updateEnvironment(env -> {
                            env.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, application.flags().isPersistent());
                            env.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, application.flags().isPublic());
                            env.setConfiguration(Environment.RepositoryConfigurationKey.KEY, application.key());
                        })
                );

    }
}
