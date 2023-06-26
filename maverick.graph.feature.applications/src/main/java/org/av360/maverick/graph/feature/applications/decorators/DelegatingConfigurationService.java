package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.ConfigurationService;
import reactor.core.publisher.Mono;

public class DelegatingConfigurationService implements ConfigurationService {

    private final ConfigurationService delegate;

    private final ApplicationsService applicationsService;

    public DelegatingConfigurationService(ConfigurationService delegate, ApplicationsService applicationsService) {
        this.delegate = delegate;
        this.applicationsService = applicationsService;
    }

    @Override
    public Mono<String> getValue(String key, SessionContext ctx) {
        String applicationLabel = ctx.getEnvironment().getScope().label();
        return applicationsService.getApplication(applicationLabel, ctx)
                .flatMap(application -> {
                    String result = application.configuration().get(key).toString();

                    if (result != null) {
                        return Mono.just(result);
                    } else {
                        return delegate.getValue(key, ctx);
                    }
                })
                .switchIfEmpty(delegate.getValue(key, ctx));
    }
}
