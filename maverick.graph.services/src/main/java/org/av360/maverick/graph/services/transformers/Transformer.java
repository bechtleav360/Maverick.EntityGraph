package org.av360.maverick.graph.services.transformers;

import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.eclipse.rdf4j.model.Model;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Transformer {

    Mono<? extends Model> handle(Model model, Map<String, String> parameters);

    default Mono<? extends Model> handle(Model model) {
        return this.handle(model, Map.of());
    }


    default void registerEntityService(EntityServices entityServices) {}

    default void registerQueryService(QueryServices queryServices) {}

    default void registerSchemaService(SchemaServices schemaServices) {}
}
