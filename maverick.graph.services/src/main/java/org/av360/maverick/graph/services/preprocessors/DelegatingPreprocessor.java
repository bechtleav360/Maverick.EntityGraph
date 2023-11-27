package org.av360.maverick.graph.services.preprocessors;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.eclipse.rdf4j.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
@Slf4j(topic = "graph.srvc.transformers.delegator")
public class DelegatingPreprocessor implements ModelPreprocessor {

    private Set<ModelPreprocessor> transformers;

    @Autowired(required = false)
    public void setRegisteredBeans(List<ModelPreprocessor> transformers) {
        this.transformers = new TreeSet<>(Comparator.comparingInt(ModelPreprocessor::getOrder));
        this.transformers.addAll(transformers);
    }


    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void registerSchemaService(SchemaServices schemaServices) {
        getRegisteredPreprocessors().forEach(preprocessor -> preprocessor.registerSchemaService(schemaServices));
    }

    @Override
    public void registerEntityService(EntityServices entityServicesImpl) {
        getRegisteredPreprocessors().forEach(preprocessor -> preprocessor.registerEntityService(entityServicesImpl));
    }

    @Override
    public void registerQueryService(QueryServices queryServices) {
        getRegisteredPreprocessors().forEach(preprocessor -> preprocessor.registerQueryService(queryServices));
    }

    @Override
    public void registerIdentifierService(IdentifierServices identifierServices) {
        getRegisteredPreprocessors().forEach(preprocessor -> preprocessor.registerIdentifierService(identifierServices));
    }

    private Set<ModelPreprocessor> getRegisteredPreprocessors() {
        if (this.transformers == null || this.transformers.isEmpty()) {
            log.warn("Default transformers are missing (not injected), check your spring configuration");
            return Set.of();
        } else return this.transformers;
    }

    @Override
    public Mono<? extends Model> handle(Model triples, Map<String, String> parameters, Environment environment) {
        if (this.transformers == null) {
            log.trace("No preprocessors registered, skip.");
            return Mono.just(triples);
        }

        return Flux.fromIterable(transformers)
                .reduce(Mono.just(triples), (modelMono, transformer) -> modelMono.map(model -> transformer.handle(model, parameters, environment))
                        .flatMap(mono -> mono)).flatMap(mono -> mono);
    }
}
