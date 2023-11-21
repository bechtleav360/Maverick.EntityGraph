package org.av360.maverick.graph.feature.applications.store;

import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.FragmentsStore;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.av360.maverick.graph.store.behaviours.Searchable;
import org.av360.maverick.graph.store.behaviours.StatementsAware;
import org.av360.maverick.graph.store.behaviours.TripleStore;

public interface ApplicationsStore extends FragmentsStore, Searchable, Maintainable, StatementsAware, TripleStore {


    @Override
    default RepositoryType getRepositoryType() {
        return RepositoryType.APPLICATION;
    }
}
