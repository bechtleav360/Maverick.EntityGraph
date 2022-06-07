package com.bechtle.cougar.graph.api.security;

import com.bechtle.cougar.graph.features.multitenancy.domain.model.ApiKey;
import lombok.Data;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public class AdminAuthentication extends AbstractAuthenticationToken {
    public static String ADMIN_AUTHORITY = "ADMIN";

    public AdminAuthentication() {
        super(AuthorityUtils.createAuthorityList(ADMIN_AUTHORITY));
    }


    @Override
    public void setDetails(Object details) {
        throw new NotImplementedException();
    }

    @Override
    public AdminAuthenticationDetails getDetails() {
        return (AdminAuthenticationDetails) super.getDetails();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "admin";
    }

    @Data
    public static class AdminAuthenticationDetails {
        private ApiKey application;
    }
}
