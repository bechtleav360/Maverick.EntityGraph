package org.av360.maverick.graph.feature.navigation.api;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.NavigationServices;
import org.eclipse.rdf4j.model.Statement;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;

@RestController
@RequestMapping(path = "/nav")
@Order(1)
@Slf4j
public class Entrypoint extends AbstractController {

    private final NavigationServices navigationServices;

    public Entrypoint(NavigationServices navigationServices) {
        this.navigationServices = navigationServices;
    }



    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> start() {
        return super.getAuthentication().flatMapMany(this.navigationServices::start);
    }

    @GetMapping(value = "/node", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> navigate(@RequestParam String id) {
        log.info("Request to navigate to node {}", id);
        if(! id.startsWith("/api"))  {
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .location(URI.create(id))
                    .build();

        } else {
            String[] split = id.substring(5).split("/");
            String key = "";
            String val = "";
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

            for (int i = 0; i < split.length; i++) {
                if(i%2 == 0) {
                    if(StringUtils.hasLength(key) && StringUtils.hasLength(val)) {
                        queryParams.set(key, val);
                        key = "";
                        val = "";
                    }
                    key = split[i];
                }
                else val = split[i];
            }
            if(StringUtils.hasLength(key) && ! StringUtils.hasLength(val)) queryParams.set(key, "list");
            if(StringUtils.hasLength(key) && StringUtils.hasLength(val)) queryParams.set(key, val);

            URI location = UriComponentsBuilder
                    .fromUri(URI.create("/nav/api"))
                    .queryParams(queryParams)
                    .build().toUri();
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .location(location)
                    .build();
        }


    }


    @GetMapping(value = "/api", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<Statement> wrap(@RequestParam MultiValueMap<String, String> params) {
        log.info("Request to navigate to params {}", params);
        return super.getAuthentication().flatMapMany(authentication -> this.navigationServices.browse(params, authentication));


    }

}
