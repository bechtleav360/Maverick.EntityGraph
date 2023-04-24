package org.av360.maverick.graph.feature.applications.domain.events;

public class ApplicationDeletedEvent extends ApplicationUpdatedEvent {

    public ApplicationDeletedEvent(String label) {
        super(label);
    }

    public String getApplicationLabel() {
        return this.getLabel();
    }


    @Override
    public String getLabel() {
        return (String) this.getSource();
    }
}
