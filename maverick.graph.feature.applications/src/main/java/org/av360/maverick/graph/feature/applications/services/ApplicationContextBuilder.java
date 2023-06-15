package org.av360.maverick.graph.feature.applications.services;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.SessionContextBuilderService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApplicationContextBuilder implements SessionContextBuilderService {

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
                    .map(applicationLabel -> context.withEnvironment().setScope(applicationLabel))
                    .flatMap(this::buildApplicationConfiguration)
                    .switchIfEmpty(Mono.just(context));

        }

    }

    private Mono<SessionContext> buildApplicationConfiguration(SessionContext context) {
        return this.applicationsService.getApplicationByLabel(context.getEnvironment().getScope(), SessionContext.SYSTEM)
                .map(application ->
                        context.withEnvironment().setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, application.flags().isPersistent())
                                .withEnvironment().setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, application.flags().isPublic())
                                .withEnvironment().setConfiguration(Environment.RepositoryConfigurationKey.KEY, application.key()));

    }
}
