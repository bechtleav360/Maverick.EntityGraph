package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.domain.model.Application;

public class DelegatingExportApplication {


    public DelegatingExportApplication() {
        super();
    }

    public Application getApplication() {
        return ReactiveApplicationContextHolder.getRequestedApplication().block();
    }
}
