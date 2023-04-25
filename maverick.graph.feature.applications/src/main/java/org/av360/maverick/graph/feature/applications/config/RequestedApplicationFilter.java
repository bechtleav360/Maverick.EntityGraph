package org.av360.maverick.graph.feature.applications.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.validation.constraints.NotNull;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Configuration
public class RequestedApplicationFilter implements WebFilter, ApplicationListener<ApplicationDeletedEvent> {


    private final ApplicationsService subscriptionsService;

    private final Cache<String, Application> cache;

    public RequestedApplicationFilter(ApplicationsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;


        this.cache = Caffeine.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    }

    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, WebFilterChain chain) {
        try {

            Optional<String> requestedApplication = this.getRequestedApplication(exchange.getRequest());
            //this.getRequestedApplicationFromPath(exchange.getRequest().getPath().toString());
            return requestedApplication
                    .map(label ->
                            ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                                    .flatMap(authentication -> this.subscriptionsService.getApplicationByLabel(label, new AdminToken()))
                                    .doOnNext(application -> this.cache.put(application.label(), application))
                                    .flatMap(application -> chain.filter(exchange)
                                            .contextWrite(ctx -> ctx.put(ReactiveApplicationContextHolder.CONTEXT_KEY, application))))
                    .orElseGet(() -> chain.filter(exchange));
            /*
            return requestedApplication
                    .map(this.cache::getIfPresent)
                    .map(application -> {

                        if(application != null) {
                            return Mono.just(application)
                                    .flatMap(a -> chain.filter(exchange))
                                    .contextWrite(ctx -> ctx.put(ReactiveApplicationContextHolder.CONTEXT_KEY, application));
                        }
                        Application ifPresent = this.cache.getIfPresent(s);

                        ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                                    .flatMap(authentication -> this.subscriptionsService.getApplication(s, authentication))
                                    .doOnNext(application -> this.cache.put(application.label(), application))
                                    .flatMap(application -> chain.filter(exchange)
                                            .contextWrite(ctx -> ctx.put(ReactiveApplicationContextHolder.CONTEXT_KEY, application))))
                    }
                    .orElseGet(() -> chain.filter(exchange));

    */
        } catch (IOException e) {
            return Mono.error(e);
        }

    }


    private Optional<String> getRequestedApplication(ServerHttpRequest request) throws IOException {
        // header OR path (path wins)
        Optional<String> fromHeader = request.getHeaders().containsKey(Globals.HEADER_APPLICATION_LABEL) ? request.getHeaders().get(Globals.HEADER_APPLICATION_LABEL).stream().findFirst() : Optional.empty();
        Optional<String> fromPath = this.getRequestedApplicationFromPath(request.getPath().toString());

        return fromPath
                .or(() -> fromHeader)
                .filter(app -> !app.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL))
                .or(Optional::empty);
    }

    /**
     * Returns the scope
     *
     * @param path request path
     * @return scope if available
     * @throws IOException if path is invalid
     */
    private Optional<String> getRequestedApplicationFromPath(String path) throws IOException {
        Assert.isTrue(StringUtils.hasLength(path), "Empty path in request details");

        String[] split = path.split("/");
        for (int i = 0; i < split.length; i++) {
            if (split[i].equalsIgnoreCase("s")) {
                if (split.length > i + 1) {
                    return Optional.of(split[i + 1]);
                } else {
                    throw new IOException("Invalid path in request, missing application label: " + path);
                }
            }
        }

        return Optional.empty();
    }


    @Override
    public void onApplicationEvent(ApplicationDeletedEvent event) {
        this.cache.invalidate(event.getApplicationLabel());
    }
}
