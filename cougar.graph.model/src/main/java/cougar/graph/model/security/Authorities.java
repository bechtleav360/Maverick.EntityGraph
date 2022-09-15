package cougar.graph.model.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class Authorities {
    public static GrantedAuthority ADMIN = new SimpleGrantedAuthority("ADMIN");
    public static GrantedAuthority USER = new SimpleGrantedAuthority("USER");
}
