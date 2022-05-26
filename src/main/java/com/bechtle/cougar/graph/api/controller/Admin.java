package com.bechtle.cougar.graph.api.controller;

import com.bechtle.cougar.graph.domain.services.AdminServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/admin/bulk")
@Api(tags = "Admin")
@Slf4j(topic = "cougar.graph.api")
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
