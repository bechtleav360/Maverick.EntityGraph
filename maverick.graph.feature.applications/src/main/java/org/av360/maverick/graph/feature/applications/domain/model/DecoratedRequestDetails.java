package org.av360.maverick.graph.feature.applications.domain.model;

import org.av360.maverick.graph.model.security.RequestDetails;

public class DecoratedRequestDetails extends RequestDetails {
    public DecoratedRequestDetails(RequestDetails requestDetails) {
        super();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "RequestApplicationDetails[]";
    }

}
