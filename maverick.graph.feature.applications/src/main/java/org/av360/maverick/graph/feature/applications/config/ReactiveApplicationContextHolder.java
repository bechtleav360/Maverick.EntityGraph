package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.domain.model.Application;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class ReactiveApplicationContextHolder {

    public static final Class<Application> CONTEXT_KEY = Application.class;

    public static Mono<Application> getRequestedApplication() {
        return Mono.deferContextual(Mono::just)
                .filter(ctx -> ctx.hasKey(CONTEXT_KEY))
                .map(ctx -> ctx.get(CONTEXT_KEY))
                .switchIfEmpty(Mono.empty());

    }

    public static Context withApplication(Application requestedApplication) {
        return Context.of(CONTEXT_KEY, requestedApplication);
    }
}
