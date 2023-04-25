package org.av360.maverick.graph.feature.applications.domain.events;

import org.av360.maverick.graph.feature.applications.domain.model.Application;

public class ApplicationCreatedEvent extends ApplicationUpdatedEvent {

    public ApplicationCreatedEvent(Application app) {
        super(app.label());
    }

    @Override
    public String getLabel() {
        return (String) this.getSource();
    }
}
