package org.av360.maverick.graph.api.config;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * We need the details from the user request at several occasions throughout the lifecycle of a request.
 *
 * For example: we need to create the hydra backlink to jump back in the navigation. This is stored in a header (set by
 * javascript)
 */
// see https://stackoverflow.com/questions/73989124/is-there-a-way-to-get-request-uri-in-spring
public class ReactiveRequestUriContextHolder {
  public static final String CONTEXT_URI_KEY = "request.uri";
  public static final String CONTEXT_HEADERS_KEY = "request.headers";


  public static Mono<URI> getURI() {
    return Mono.deferContextual(Mono::just).map(ctx -> ctx.get(CONTEXT_URI_KEY));
  }

  public static Mono<MultiValueMap<String, String>> getHeaders() {
    return Mono.deferContextual(Mono::just)
            .map(ctx -> ctx.get(CONTEXT_HEADERS_KEY))
            .map(headers -> {
              if(headers instanceof HttpHeaders httpHeaders) {
                return httpHeaders;
              } else return null;
            });
  }
}