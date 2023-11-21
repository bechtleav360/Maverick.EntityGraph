package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.enums.RepositoryType;

public interface ClassifierStore extends FragmentsStore {

    @Override
    default RepositoryType getRepositoryType() {
        return RepositoryType.CLASSIFIER;
    }
}
