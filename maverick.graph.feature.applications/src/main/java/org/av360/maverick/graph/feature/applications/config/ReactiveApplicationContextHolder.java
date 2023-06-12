package org.av360.maverick.graph.feature.applications.config;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j(topic = "graph.feat.app.context")
public class ReactiveApplicationContextHolder {


    public record ApplicationLabel(String label) {}

    public static final String CONTEXT_LABEL_KEY = "application.label";


    public static Mono<String> getRequestedApplicationLabel() {
        return Mono.deferContextual(Mono::just)
                .filter(ctx -> ctx.hasKey(CONTEXT_LABEL_KEY))
                .map(ctx -> ctx.get(CONTEXT_LABEL_KEY).toString())
                .map(label -> label)
                .switchIfEmpty(Mono.empty())
                .doOnError(error -> log.error("Failed to read application label from context due to error: {}", error.getMessage()));

    }



    public static Context withApplicationLabel(String requestedApplication) {
        return Context.of(CONTEXT_LABEL_KEY, requestedApplication);
    }


}
