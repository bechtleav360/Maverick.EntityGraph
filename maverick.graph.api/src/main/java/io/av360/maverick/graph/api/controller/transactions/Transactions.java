package io.av360.maverick.graph.api.controller.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Api to request previous transactions
 */
@RestController
@RequestMapping(path = "/api/transactions")
//@Api(tags = "Transactions")
@Slf4j(topic = "graph.ctrl.api.transactions")
@SecurityRequirement(name = "api_key")
public class Transactions extends AbstractController {

    protected final ObjectMapper objectMapper;
    protected final EntityServices graphService;

    public Transactions(ObjectMapper objectMapper, EntityServices graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    //@ApiOperation(value = "Read transaction")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id) {
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        // FIXME: marker to use transactions repository
        return super.getAuthentication()
                .flatMap(authentication -> graphService.get(id, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isTraceEnabled()) log.trace("Reading transaction with id: {}", id);
                });
    }

}
