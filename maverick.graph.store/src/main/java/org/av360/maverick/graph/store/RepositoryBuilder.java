package org.av360.maverick.graph.store;

import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.springframework.security.core.Authentication;

import java.io.IOException;

public interface RepositoryBuilder {


    LabeledRepository buildRepository(RepositoryType repositoryType, Authentication authentication) throws IOException;


}
