package org.av360.maverick.graph.api.controller.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.TransactionsService;
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
public class TransactionsController extends AbstractController {

    protected final ObjectMapper objectMapper;
    protected final TransactionsService transactionsService;


    public TransactionsController(ObjectMapper objectMapper, TransactionsService transactionsService) {
        this.objectMapper = objectMapper;
        this.transactionsService = transactionsService;
    }

    //@ApiOperation(value = "Read transaction")
    @GetMapping(value = "/{id:[\\w|\\d|\\-|\\_]+}", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> read(@PathVariable String id) {
        Assert.isTrue(id.length() == LocalIdentifier.LENGTH, "Incorrect length for identifier.");

        // FIXME: marker to use transactions repository
        return super.acquireContext()
                .flatMap(ctx -> transactionsService.find(id, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isTraceEnabled()) log.trace("Reading transaction with id: {}", id);
                });
    }

    @GetMapping(value = "", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> list(@RequestParam(value = "limit", defaultValue = "100") Integer limit,
                                  @RequestParam(value = "offset", defaultValue = "0") Integer offset) {

        // FIXME: marker to use transactions repository
        return super.acquireContext()
                .flatMapMany(ctx -> transactionsService.list(limit, offset, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isTraceEnabled()) log.trace("Listing last {} transactions with offset {}", limit, offset);
                });
    }

}
