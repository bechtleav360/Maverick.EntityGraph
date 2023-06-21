package org.av360.maverick.graph.model.context;

import org.springframework.http.server.reactive.ServerHttpRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RequestDetails {



    @Nullable
    private String path;
    @Nonnull
    private Map<String, String> headers;
    @Nonnull
    private Map<String, String> parameter;

    private Map<String, String> configuration;

    public RequestDetails () {
        this.configuration = new HashMap<>();
        this.parameter = new HashMap<>();
        this.headers = new HashMap<>();
    }


    public static RequestDetails withRequest(ServerHttpRequest request) {
        return new RequestDetails()
                .setParameter(request.getQueryParams().toSingleValueMap())
                .setPath(request.getPath().toString())
                .setHeaders(request.getHeaders().toSingleValueMap());
    }


    private Map<String, String> normalize(Map<String, String> headers) {
        if(headers == null) return Map.of();

        Map<String, String> normalized = new HashMap<>();
        headers.forEach((key, val) -> normalized.put(key.toUpperCase(), val));
        return normalized;
    }
    @Nullable
    public String getPath() {
        return path;
    }

    public RequestDetails setPath(@Nullable String path) {
        this.path = path;
        return this;
    }

    @Nonnull
    public Map<String, String> getHeaders() {
        return headers;
    }

    public RequestDetails setHeaders(@Nonnull Map<String, String> headers) {
        this.headers = normalize(headers);
        return this;
    }

    @Nonnull
    public Map<String, String> getParameter() {
        return parameter;
    }

    public RequestDetails setParameter(@Nonnull Map<String, String> parameter) {
        this.parameter = normalize(parameter);
        return this;
    }


    public void setConfiguration(Serializable key, Serializable value) {
        this.getConfiguration().put(key.toString(), value.toString());
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }
}
