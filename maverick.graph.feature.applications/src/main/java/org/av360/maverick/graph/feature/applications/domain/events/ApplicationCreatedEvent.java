package org.av360.maverick.graph.feature.applications.domain.events;

import org.av360.maverick.graph.feature.applications.domain.model.Application;

public class ApplicationCreatedEvent extends GraphApplicationEvent {

    private final Application application;

    public ApplicationCreatedEvent(Application app) {
        super(app.label());
        this.application = app;
    }

    @Override
    public String getLabel() {
        return (String) this.getSource();
    }

    public Application getApplication() {
        return this.application;
    }
}
