package org.av360.maverick.graph.feature.applications.config;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j(topic = "graph.feat.app.context")
public class ReactiveApplicationContextHolder {

    public record ApplicationLabel(String label) {}

    public static final Class<ApplicationLabel> CONTEXT_LABEL_KEY = ApplicationLabel.class;


    public static final Class<Application> CONTEXT_APP_KEY = Application.class;

    public static Mono<Application> getRequestedApplication() {
        return Mono.deferContextual(Mono::just)
                .filter(ctx -> ctx.hasKey(CONTEXT_APP_KEY))
                .map(ctx -> ctx.get(CONTEXT_APP_KEY))
                .switchIfEmpty(Mono.empty())
                .doOnError(error -> log.error("Failed to read application from context due to error: {}", error.getMessage()));

    }

    public static Mono<String> getRequestedApplicationLabel() {
        return getRequestedApplication()
                .map(Application::label)
                .switchIfEmpty(Mono.deferContextual(Mono::just)
                        .filter(ctx -> ctx.hasKey(CONTEXT_LABEL_KEY))
                        .map(ctx -> ctx.get(CONTEXT_LABEL_KEY))
                        .map(ApplicationLabel::label)
                )
                .switchIfEmpty(Mono.empty())
                .doOnError(error -> log.error("Failed to read application label from context due to error: {}", error.getMessage()));

    }



    public static Context withApplication(Application requestedApplication) {
        return Context.of(CONTEXT_APP_KEY, requestedApplication);
    }

}
