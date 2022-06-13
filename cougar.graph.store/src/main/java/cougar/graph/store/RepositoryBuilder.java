package cougar.graph.store;

import org.eclipse.rdf4j.repository.Repository;
import org.springframework.security.core.Authentication;

import java.io.IOException;

public interface RepositoryBuilder {

    Repository buildRepository(RepositoryType repositoryType, Authentication authentication) throws IOException;
}
