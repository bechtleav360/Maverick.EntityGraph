package org.av360.maverick.graph.model.events;

import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.SessionContext;
import org.springframework.context.ApplicationEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JobScheduledEvent extends ApplicationEvent {
    private final SessionContext ctx;

    public JobScheduledEvent(@Nonnull String name, SessionContext ctx) {
        super(name);

        this.ctx = ctx;

        Validate.notNull(ctx);
        Validate.notNull(ctx.getEnvironment());
        Validate.notBlank(name, "Job Event without name");
    }

    public String getJobName() {
        return (String) super.getSource();
    }

    public String getScope() {
        return ctx.getEnvironment().getScope().label();
    }


    public String getJobIdentifier() {
        return String.format("%s:%s", getJobName(), getScope());
    }

    public SessionContext getSessionContext() {
        return ctx;
    }


    @Override
    public String toString() {
        return this.getJobIdentifier(); 
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
