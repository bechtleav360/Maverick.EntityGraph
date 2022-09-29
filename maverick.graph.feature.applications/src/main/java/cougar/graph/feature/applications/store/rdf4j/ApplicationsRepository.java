package cougar.graph.feature.applications.store.rdf4j;

import cougar.graph.feature.applications.store.ApplicationsStore;
import cougar.graph.store.RepositoryType;
// FIXME: runtime dependency on rdf4j feature is annoying
import cougar.graph.store.rdf4j.repository.util.AbstractRepository;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class ApplicationsRepository extends AbstractRepository implements ApplicationsStore {


    public ApplicationsRepository() {
        super(RepositoryType.APPLICATION);
    }


}
