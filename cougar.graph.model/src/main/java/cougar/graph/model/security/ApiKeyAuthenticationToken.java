package cougar.graph.model.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.*;

/**
 * The initial authentication, only giving access to the Api Key itself. Is later replaced by the Subscription or
 * Admin authentication (depending on the key stored here).
 */
public class ApiKeyAuthenticationToken implements Authentication {

    public static final String API_KEY_HEADER = "X-API-KEY";
    private final List<GrantedAuthority> authorities;

    private final Map<String, String> headers;
    private boolean isAuthenticated;


    public ApiKeyAuthenticationToken(Map<String, String> headers) {
        this.headers = headers;
        this.authorities = AuthorityUtils.NO_AUTHORITIES;
    }

    public ApiKeyAuthenticationToken() {
        this(new HashMap<>());
    }

    @Override
    public Map<String, String> getDetails() {
        return this.headers;
    }

    public Optional<String> getApiKey() {
        return Optional.of(this.getDetails().get(API_KEY_HEADER));
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.authorities;
    }


    /**
     * Since multiple authentication managers might need different authorities, we keep this list mutable.
     * @param authority
     */
    public void grantAuthority(GrantedAuthority authority) {
        // authorities USER and ADMIN are XOR
        if(authority == Authorities.USER && this.getAuthorities().contains(Authorities.ADMIN)) throw new SecurityException("Granting user authority while admin authority has been set already");
        if(authority == Authorities.ADMIN && this.getAuthorities().contains(Authorities.USER)) throw new SecurityException("Granting admin authority while user authority has been set already");

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