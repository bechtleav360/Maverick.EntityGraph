package org.av360.maverick.graph.model.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JobScheduledEvent extends ApplicationEvent {
    private Authentication token;

    public JobScheduledEvent(@Nonnull String name, Authentication authentication) {
        super(name);
        this.token = authentication;

        if(!StringUtils.hasLength(name)) throw  new IllegalArgumentException("Job Event without name");
    }

    public String getJobName() {
        return (String) super.getSource();
    }


    public String getJobIdentifier() {
        return String.format("%s:%s", getJobName(), "default");
    }

    public Context buildContext(Context ctx) {
        return ctx.putAll(ReactiveSecurityContextHolder.withAuthentication(this.getToken()).readOnly());
    }

    public Authentication getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobScheduledEvent event)) return false;
        return event.getJobIdentifier().equalsIgnoreCase(getJobIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getJobIdentifier());
    }
}
