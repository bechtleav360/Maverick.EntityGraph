package cougar.graph.store;

import cougar.graph.store.behaviours.*;
import org.eclipse.rdf4j.model.*;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import cougar.graph.store.rdf.models.Entity;


public interface EntityStore extends Searchable, Resettable, ModelUpdates, Selectable, Statements {

    Mono<Entity> getEntity(IRI id, Authentication authentication);











}
