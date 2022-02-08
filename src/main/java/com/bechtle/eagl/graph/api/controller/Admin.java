package com.bechtle.eagl.graph.api.controller;

import com.bechtle.eagl.graph.api.converter.RdfUtils;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.services.AdminServices;
import com.bechtle.eagl.graph.domain.services.QueryServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/admin")
@Api(tags = "Admin")
@Slf4j
public class Admin {
    protected final AdminServices adminServices;

    public Admin(AdminServices adminServices) {
        this.adminServices = adminServices;
    }

    @ApiOperation(value = "Empty repository", tags = {"v1"})
    @GetMapping(value = "/reset", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> queryBindings() {
        log.warn("(Request) Clearing the repository");
        return adminServices.reset(); // .map(ResponseEntity::ok);
    }


    @ApiOperation(value = "Import RDF into entity repository", tags = {"v1"})
    @PostMapping(value = "/import/entities", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importEntities(@RequestBody Flux<DataBuffer> bytes, @RequestParam String mimetype) {
        log.info("(Request) Importing a file of mimetype {}", mimetype);
        Assert.isTrue(StringUtils.hasLength(mimetype), "Mimetype is a required parameter");


        return adminServices.importEntities(bytes, mimetype); // .map(ResponseEntity::ok);

    }


}
