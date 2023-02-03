package io.av360.maverick.graph.model.security;

import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.*;

/**
 * The initial authentication, only giving access to the Api Key itself. Is later replaced by the Subscription or
 * Admin authentication (depending on the key stored here).
 */
public class ApiKeyAuthenticationToken implements Authentication {

    public static final String API_KEY_HEADER = "X-API-KEY";
    private final Set<Authorities.WeightedAuthority> authorities;

    private final Map<String, String> headers;
    private boolean isAuthenticated;


    public ApiKeyAuthenticationToken(Map<String, String> headers) {


        this.headers = new HashMap<>();
        // headers are case insensitive according to RFC 2616
        headers.forEach((key, val) -> this.headers.put(key.toUpperCase(), val));

        this.authorities = new HashSet<>(Authorities.NO_AUTHORITIES);
    }

    public ApiKeyAuthenticationToken() {
        this(new HashMap<>());
    }

    @Override
    public Map<String, String> getDetails() {
        return this.headers;
    }

    public Optional<String> getApiKey() {
        return Optional.ofNullable(Objects.requireNonNull(this.getDetails()).get(API_KEY_HEADER));
    }

    @Override
    public Collection<Authorities.WeightedAuthority> getAuthorities() {
        return this.authorities;
    }


    /**
     * Since different authentication managers might need different authorities, we keep this list mutable.
     *
     * @param authority
     */
    public void grantAuthority(Authorities.WeightedAuthority authority) {
        // authorities USER and ADMIN are XOR
        // if(authority == Authorities.USER && this.getAuthorities().contains(Authorities.ADMIN)) throw new SecurityException("Granting user authority while admin authority has been set already");
        // if(authority == Authorities.ADMIN && this.getAuthorities().contains(Authorities.USER)) throw new SecurityException("Granting admin authority while user authority has been set already");

        this.getAuthorities().add(authority);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.getApiKey();
    }

    @Override
    public boolean isAuthenticated() {
        return this.isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }


    @Override
    public String getName() {
        return "API Key";
    }
}