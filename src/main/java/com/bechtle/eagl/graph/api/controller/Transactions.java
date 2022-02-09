package com.bechtle.eagl.graph.api.controller;

import com.bechtle.eagl.graph.domain.model.enums.RdfMimeTypes;
import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.domain.model.wrapper.IncomingStatements;
import com.bechtle.eagl.graph.domain.services.EntityServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

/**
 * Api to request previous transactions
 */
@RestController
@RequestMapping(path = "/api/transactions")
@Api(tags = "Transactions")
@Slf4j
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
                .flatMapIterable(AbstractModelWrapper::asStatements);
    }

}
