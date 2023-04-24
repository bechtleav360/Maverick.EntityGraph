package org.av360.maverick.graph.model.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.util.context.Context;

public class JobScheduledEvent extends ApplicationEvent {
    private Authentication token;

    public JobScheduledEvent(String name, Authentication authentication) {
        super(name);
        this.token = authentication;
    }

    public String getJobName() {
        return (String) super.getSource();
    }

    public String getJobIdentifier() {
        return (String) super.getSource();
    }

    public Context buildContext(Context ctx) {
        return ctx.putAll(ReactiveSecurityContextHolder.withAuthentication(this.getToken()).readOnly());
    }

    public Authentication getToken() {
        return token;
    }
}
