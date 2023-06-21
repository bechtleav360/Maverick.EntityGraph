package org.av360.maverick.graph.feature.applications.services.events;

import org.av360.maverick.graph.feature.applications.services.model.Application;

public class ApplicationUpdatedEvent extends GraphApplicationEvent {

    Application application;

    public ApplicationUpdatedEvent(Application app) {
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
