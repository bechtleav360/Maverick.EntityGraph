package cougar.graph.services.services.handler;

import cougar.graph.services.services.EntityServices;
import cougar.graph.store.rdf.models.AbstractModel;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends AbstractModel> handle(EntityServices entityServices, AbstractModel model, Map<String, String> parameters);
}
