package org.av360.maverick.graph.feature.applications.store;

import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.av360.maverick.graph.store.behaviours.Searchable;
import org.av360.maverick.graph.store.behaviours.StatementsAware;
import org.av360.maverick.graph.store.behaviours.TripleStore;

public interface ApplicationsStore extends EntityStore, Searchable, Maintainable, StatementsAware, TripleStore {

}
