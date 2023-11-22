package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.store.RepositoryBuilder;

public interface TripleStore extends RepositoryBehaviour {



    String getDirectory();

    RepositoryBuilder getBuilder();


}
