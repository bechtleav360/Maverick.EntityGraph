package org.av360.maverick.graph.services.config;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.security.Authorities;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuthorizeKey {


    @Around("@annotation(RequiresPrivilege)")
    public Object authorize(ProceedingJoinPoint joinPoint) throws Throwable {

        Optional<SessionContext> sessionContextOptional = Arrays.stream(joinPoint.getArgs()).filter(o -> o instanceof SessionContext).findFirst().map(obk -> (SessionContext) obk);
        if(sessionContextOptional.isEmpty()) throw new IllegalArgumentException("Missing Session Context while running authorization");
        SessionContext ctx = sessionContextOptional.get();


        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String requiredAuthority = signature.getMethod().getAnnotation(RequiresPrivilege.class).value();

        if (StringUtils.isEmpty(requiredAuthority)) {
            ctx.withAuthorization(new AuthorizationDecision(false));
            throw new UnsupportedOperationException("Missing required authority while access a repository.");
        }

        if (ctx.getAuthentication().isPresent()) {
            if(!ctx.getAuthentication().get().isAuthenticated()) {
                ctx.withAuthorization(new AuthorizationDecision(false));
                throw new IllegalArgumentException("Request is not authenticated, skipping authorization");
            }

            if (!Authorities.satisfies(requiredAuthority, ctx.getAuthentication().get().getAuthorities())) {
                String msg = String.format("Required authority '%s' not met for authentication with authorities '%s'", requiredAuthority, ctx.getAuthentication().get().getAuthorities());
                ctx.withAuthorization(new AuthorizationDecision(false));
                throw new InsufficientPrivilegeException(msg);
            } else {
                Set<GrantedAuthority> granted = ctx.getAuthentication().get().getAuthorities().stream().map(grantedAuthority -> (GrantedAuthority) grantedAuthority).collect(Collectors.toSet());
                ctx.withAuthorization(new AuthorityAuthorizationDecision(true, granted));
            }
        } else {
            ctx.withAuthorization(new AuthorizationDecision(false));
            throw new UnsupportedOperationException("Missing required authentication in session context for authorization .");
        }

        return joinPoint.proceed();
    }


}
