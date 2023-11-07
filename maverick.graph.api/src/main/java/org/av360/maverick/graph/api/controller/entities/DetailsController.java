package org.av360.maverick.graph.api.controller.entities;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.DetailsAPI;
import org.av360.maverick.graph.model.enums.PropertyType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.ValueServices;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@Slf4j(topic = "graph.api.ctrl.details")
public class DetailsController extends AbstractController implements DetailsAPI {



    protected final ValueServices values;

    protected final EntityServices entities;

    public DetailsController(ValueServices values, EntityServices entities) {
        this.values = values;
        this.entities = entities;
    }

    @Override
    public Flux<AnnotatedStatement> getDetails(
            String id,
            PropertyType type,
            String prefixedValueKey,
            boolean hash
    ) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }



    @Override
    public Flux<AnnotatedStatement> remove(
            String key,
            PropertyType type,
            String prefixedProperty,
            String prefixedDetailProperty,
            String valueIdentifier) {
        return super.acquireContext()
                .flatMap(ctx -> values.removeDetail(key, prefixedProperty, prefixedDetailProperty, valueIdentifier, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Deleted property '{}' of entity '{}'", prefixedProperty, key);
                });
    }

    @Override
    public Flux<AnnotatedStatement> insert(
            String key,
            PropertyType type,
            String prefixedProperty,
            String prefixedDetailProperty,
            String valueIdentifier,
            String value
    ) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");
        return super.acquireContext()
                .flatMap(ctx -> values.insertDetail(key, prefixedProperty, prefixedDetailProperty, value, valueIdentifier , ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to add detail '{}' on property '{}' for entity '{}' with value: {}", prefixedDetailProperty, prefixedProperty, key, value.length() > 64 ? value.substring(0, 64) : value);
                });

    }





}

