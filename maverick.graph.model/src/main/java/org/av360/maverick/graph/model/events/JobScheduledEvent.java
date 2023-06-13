package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.context.SessionContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JobScheduledEvent extends ApplicationEvent {
    private final SessionContext ctx;

    public JobScheduledEvent(@Nonnull String name, SessionContext ctx) {
        super(name);
        this.ctx = ctx;

        if(!StringUtils.hasLength(name)) throw  new IllegalArgumentException("Job Event without name");
    }

    public String getJobName() {
        return (String) super.getSource();
    }

    public String getScope() {
        return "default";
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
