package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.entities.Job;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;

public class JobScheduledEvent extends ApplicationEvent {
    private Authentication token;

    public JobScheduledEvent(Job scheduledJob) {
        super(scheduledJob);
    }

    public Job getScheduledJob() {
        return (Job) super.getSource();
    }

    public void setAuthentication(Authentication token) {
        this.token = token;
    }

    public Authentication getToken() {
        return token;
    }
}
