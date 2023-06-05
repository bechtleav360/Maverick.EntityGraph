package org.av360.maverick.graph.feature.applications.config;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.model.util.PreAuthenticationWebFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;


@Component
@Order(1)
@Slf4j(topic = "graph.feat.apps.filter")
public class RequestedApplicationFilter implements PreAuthenticationWebFilter {


    private final ApplicationsService subscriptionsService;

    public RequestedApplicationFilter(ApplicationsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }

    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, WebFilterChain chain) {

        try {

            // FIXME: remove repo request from filter, only extract label from path and store it as string. Let it handle downstream

            // we only store the label in the context... writing the actual application into the context has to happen later

            return getRequestedApplication(exchange.getRequest())
                    .map(label ->   {
                        log.trace("Extracted requested application '{}' from request.", label);
                        return chain.filter(exchange).contextWrite(context -> context.put(ReactiveApplicationContextHolder.CONTEXT_LABEL_KEY, new ReactiveApplicationContextHolder.ApplicationLabel(label)));

                    })
                    .orElseGet(() -> chain.filter(exchange));

                        //this.getRequestedApplicationFromPath(exchange.getRequest().getPath().toString());
            /* return requestedApplication
                    .map(label ->
                            this.subscriptionsService.getApplicationByLabel(label, new AdminToken())
                                            .flatMap(application -> chain.filter(exchange).contextWrite(context -> context.put(ReactiveApplicationContextHolder.CONTEXT_APP_KEY, application))))
                    .orElseGet(() -> chain.filter(exchange));
             */
        } catch (IOException e) {
            return Mono.error(e);
        }

    }


    private Optional<String> getRequestedApplication(ServerHttpRequest request) throws IOException {
        return getRequestedApplication(request.getPath().toString(), request.getHeaders().toSingleValueMap(), request.getQueryParams().toSingleValueMap());


    }

    public static Optional<String> getRequestedApplication(String path, Map<String, String> headers, Map<String, String> queryParams) throws IOException {
        // header OR path (path wins)
        Optional<String> fromHeader = getRequestedApplicationFromHeaders(headers);
        Optional<String> fromPath = getRequestedApplicationFromPath(path);
        Optional<String> fromParameter = getRequestedApplicationFromQueryParam(queryParams);

        return fromPath
                .or(() -> fromHeader)
                .or(() -> fromParameter)
                .filter(app -> !app.equalsIgnoreCase(Globals.DEFAULT_APPLICATION_LABEL))
                .or(Optional::empty);

    }

    public static Optional<String> getRequestedApplicationFromHeaders(Map<String, String> headers) {
        return headers.containsKey(Globals.HEADER_APPLICATION_LABEL) ? Optional.ofNullable(headers.get(Globals.HEADER_APPLICATION_LABEL)) : Optional.empty();
    }

    public static Optional<String> getRequestedApplicationFromQueryParam(Map<String, String> queryParams) {
        return Optional.ofNullable(queryParams.get("s"));
    }

    /**
     * Returns the scope
     *
     * @param path request path
     * @return scope if available
     * @throws IOException if path is invalid
     */
    public static Optional<String> getRequestedApplicationFromPath(String path) throws IOException {
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


}
