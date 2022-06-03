package com.bechtle.cougar.graph.api.controller;

import com.bechtle.cougar.graph.domain.services.AdminServices;
import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
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

import java.util.Locale;

@RestController
@RequestMapping(path = "/api/admin/bulk")
@Api(tags = "Admin")
@Slf4j(topic = "cougar.graph.api")
public class Admin extends AbstractController {
    protected final AdminServices adminServices;

    public Admin(AdminServices adminServices) {
        this.adminServices = adminServices;
    }

    @ApiOperation(value = "Empty repository", tags = {"v1"})
    @GetMapping(value = "/reset", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> queryBindings(@RequestParam(name = "name") String repositoryTypeName) {
        RepositoryConfiguration.RepositoryType repositoryType;
        if(!StringUtils.hasLength(repositoryTypeName)) repositoryType = RepositoryConfiguration.RepositoryType.ENTITIES;
        else repositoryType = RepositoryConfiguration.RepositoryType.valueOf(repositoryTypeName.toUpperCase(Locale.ROOT));

        Assert.notNull(repositoryType, "Invalid value for repository type: "+ repositoryTypeName);

        return super.getAuthentication()
                .map(auth -> adminServices.reset(auth, repositoryType))
                .then()
                .doOnSubscribe(s -> log.debug("(Request) Clearing the repository"));
    }


    @ApiOperation(value = "Import RDF into entity repository", tags = {"v1"})
    @PostMapping(value = "/import/entities", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importEntities(@RequestBody Flux<DataBuffer> bytes, @RequestParam String mimetype) {
        Assert.isTrue(StringUtils.hasLength(mimetype), "Mimetype is a required parameter");

        return super.getAuthentication()
                .map(authentication -> adminServices.importEntities(bytes, mimetype, authentication)).then()
                .doOnSubscribe(s -> log.debug("(Request) Importing a file of mimetype {}", mimetype));
    }


}
