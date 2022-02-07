package com.bechtle.eagl.graph.api.controller;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.services.AdminServices;
import com.bechtle.eagl.graph.domain.services.QueryServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/admin")
@Api(tags = "Queries")
@Slf4j
public class Admin {
    protected final AdminServices adminServices;

    public Admin(AdminServices adminServices) {
        this.adminServices = adminServices;
    }

    @ApiOperation(value = "Empty repository", tags = {"v1", "entity"})
    @GetMapping(value = "/reset", produces = {"text/plain"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> queryBindings() {
        log.warn("(Request) Clearing the repository");
        return adminServices.reset(); // .map(ResponseEntity::ok);
    }


}
