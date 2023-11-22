package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.enums.RepositoryType;

public interface IndividualsStore extends FragmentsStore {

    @Override
    default RepositoryType getRepositoryType() {
        return RepositoryType.ENTITIES;
    }
}
