package com.bechtle.eagl.graph.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public class AdminAuthentication extends AbstractAuthenticationToken {
    public static String ADMIN_AUTHORITY = "ADMIN";


    public AdminAuthentication() {
        super(AuthorityUtils.createAuthorityList(ADMIN_AUTHORITY));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "admin";
    }
}
