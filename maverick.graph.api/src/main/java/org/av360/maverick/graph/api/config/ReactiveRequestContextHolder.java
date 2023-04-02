package org.av360.maverick.graph.api.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.URI;

// see https://stackoverflow.com/questions/73989124/is-there-a-way-to-get-request-uri-in-spring
public class ReactiveRequestContextHolder {
  public static final Class<ServerHttpRequest> CONTEXT_KEY = ServerHttpRequest.class;

  public static Mono<ServerHttpRequest> getRequest() {
    return Mono.deferContextual(Mono::just).map(ctx -> ctx.get(CONTEXT_KEY));
  }

  public static Mono<URI> getURI() {
    return getRequest().map(HttpRequest::getURI);
  }
}