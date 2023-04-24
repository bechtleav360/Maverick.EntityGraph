package org.av360.maverick.graph.api.config;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpHeaders;

// see https://stackoverflow.com/questions/73989124/is-there-a-way-to-get-request-uri-in-spring
public class ReactiveRequestUriContextHolder {
  public static final Class<URI> CONTEXT_URI_KEY = URI.class;
  public static final Class<HttpHeaders> CONTEXT_HEADERS_KEY = HttpHeaders.class;


  public static Mono<URI> getURI() {
    return Mono.deferContextual(Mono::just).map(ctx -> ctx.get(CONTEXT_URI_KEY));
  }

  public static Mono<HttpHeaders> getHeaders() {
    return Mono.deferContextual(Mono::just).map(ctx -> ctx.get(CONTEXT_HEADERS_KEY));
  }
}