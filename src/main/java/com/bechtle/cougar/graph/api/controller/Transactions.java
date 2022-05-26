package com.bechtle.cougar.graph.api.controller;

import com.bechtle.cougar.graph.domain.model.enums.RdfMimeTypes;
import com.bechtle.cougar.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Slf4j(topic = "cougar.graph.api")
public class Transactions {

    protected final ObjectMapper objectMapper;
    protected final EntityServices graphService;

    public Transactions(ObjectMapper objectMapper, EntityServices graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    @ApiOperation(value = "Read transaction", tags = {"v2"})
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}", produces = { RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE, RdfMimeTypes.N3_VALUE })
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id) {
        log.trace("(Request) Reading transaction with id: {}", id);
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        // FIXME: marker to use transactions repository
        return graphService.readEntity(id)
                .flatMapIterable(AbstractModel::asStatements);
    }

}
