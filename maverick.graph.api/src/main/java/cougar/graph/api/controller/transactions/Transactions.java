package cougar.graph.api.controller.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import cougar.graph.api.controller.AbstractController;
import cougar.graph.model.enums.RdfMimeTypes;
import cougar.graph.model.rdf.GeneratedIdentifier;
import cougar.graph.model.rdf.NamespaceAwareStatement;
import cougar.graph.store.rdf.models.AbstractModel;
import cougar.graph.services.services.EntityServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "Transactions")
@Slf4j(topic = "graph.api.transactions")
public class Transactions extends AbstractController {

    protected final ObjectMapper objectMapper;
    protected final EntityServices graphService;

    public Transactions(ObjectMapper objectMapper, EntityServices graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    @ApiOperation(value = "Read transaction")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id) {
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        // FIXME: marker to use transactions repository
        return super.getAuthentication()
                .flatMap(authentication -> graphService.readEntity(id, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isTraceEnabled()) log.trace("Reading transaction with id: {}", id);
                });
    }

}
