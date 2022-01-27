package com.bechtle.eagl.graph.api;


import com.apicatalog.jsonld.JsonLdError;
import com.bechtle.eagl.graph.model.Triples;
import com.bechtle.eagl.graph.repository.VolatileRepository;
import com.bechtle.eagl.graph.services.Graph;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/entities")
@Api( tags = "Entities")
@Slf4j
public class Entities {

    private final ObjectMapper objectMapper;
    private final Graph graphService;

    public Entities(ObjectMapper objectMapper, Graph graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    @GetMapping("/{entityId}")
    @ResponseStatus(HttpStatus.OK)
    public void get(@PathVariable String entityId) {

    }


    @PostMapping(value = "", consumes = "text/turtle")
    @ResponseStatus(HttpStatus.OK)
    public void createWithRDF(@RequestBody ObjectNode json) {


    }

    @PostMapping(value = "", consumes = "application/ld+json")
    @ResponseStatus(HttpStatus.OK)
    public void createWithJsonLd(@RequestBody ObjectNode json, @Autowired VolatileRepository repository) throws IOException {
        this.graphService.create(Triples.of(json, this.objectMapper));


    }

}
