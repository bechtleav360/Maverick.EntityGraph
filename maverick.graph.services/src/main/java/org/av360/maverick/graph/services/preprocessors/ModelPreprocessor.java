package org.av360.maverick.graph.services.preprocessors;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.eclipse.rdf4j.model.Model;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Preprocessing steps before committing the statements to the graph.
 */
public interface ModelPreprocessor {

    int getOrder();

    Mono<? extends Model> handle(Model model, Map<String, String> parameters, Environment environment);

    default Mono<? extends Model> handle(Model model, Environment environment) {
        return this.handle(model, Map.of(), environment);
    }



    default void registerEntityService(EntityServices entityServices) {}

    default void registerQueryService(QueryServices queryServices) {}

    default void registerSchemaService(SchemaServices schemaServices) {}

    default void registerIdentifierService(IdentifierServices identifierServices) {}

}
