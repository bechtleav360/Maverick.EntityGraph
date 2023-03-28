package io.av360.maverick.graph.model.security;

import org.springframework.http.server.reactive.ServerHttpRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public record RequestDetails(@Nullable String path,
                             @Nonnull Map<String, String> headers,
                             @Nonnull Map<String, String> parameter) {

    public RequestDetails {
        headers = normalize(headers);
        parameter = normalize(parameter);
    }



    public static RequestDetails withRequest(ServerHttpRequest request) {
        return new RequestDetails(request.getPath().toString(), request.getHeaders().toSingleValueMap(), request.getQueryParams().toSingleValueMap());

    }


    private Map<String, String> normalize(Map<String, String> headers) {
        if(headers == null) return Map.of();

        Map<String, String> normalized = new HashMap<>();
        headers.forEach((key, val) -> normalized.put(key.toUpperCase(), val));
        return normalized;
    }



}
