package org.av360.maverick.graph.api.controller.navigate;

import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.NavigationServices;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/")
@Order(1)
public class Entrypoint {

    private final NavigationServices navigationServices;

    public Entrypoint(NavigationServices navigationServices) {
        this.navigationServices = navigationServices;
    }

    @GetMapping(produces = RdfMimeTypes.JSONLD_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> start() {
        return this.navigationServices.start();
    }

}
