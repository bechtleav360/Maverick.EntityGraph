package org.av360.maverick.graph.feature.navigation.controller;

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
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping(path = "/nav")
@Order(1)
@Slf4j
public class NavigationRestController extends AbstractController {

    private final NavigationServices navigationServices;

    public NavigationRestController(NavigationServices navigationServices) {
        this.navigationServices = navigationServices;
    }



    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> start() {
        return super.acquireContext().flatMapMany(this.navigationServices::start);
    }

    @GetMapping(value = "/s/{scope}/entities", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<Statement> navigateToScopeEntryPoint(
            @PathVariable("scope") String scope,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset) {

        Map<String, String> config = new HashMap<>();
        if(Objects.nonNull(limit)) config.put("limit", String.valueOf(limit.intValue()));
        if(Objects.nonNull(offset)) config.put("offset", String.valueOf(offset.intValue()));

        return super
                .acquireContext()
                .doOnSubscribe(sub -> log.info("Request to navigate to entry point for scope {}", scope))
                .flatMapMany(context -> this.navigationServices.list(config, context, null));
    }

    @GetMapping(value = "/s/{scope}/entities/{key}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<Statement> navigateToViewEntityEntryPoint(@PathVariable("scope") String scope, @PathVariable("key") String key) {
        Map<String, String> config = new HashMap<>();
        config.put("entities", "view");
        config.put("key", key);

        return super.acquireContext()
                .doOnSubscribe(sub -> log.info("Request to navigate to entity {} in  scope {}", key, scope))
                .flatMapMany(context -> this.navigationServices.browse(config, context));
    }

    @GetMapping(value = "/entities/{key}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<Statement> navigateToViewEntityEntryPoint(@PathVariable("key") String key) {
        Map<String, String> config = new HashMap<>();
        config.put("entities", "view");
        config.put("key", key);

        return super.acquireContext()
                .doOnSubscribe(sub -> log.info("Request to navigate to entity {} in default scope", key))
                .flatMapMany(context -> this.navigationServices.browse(config, context));
    }


    @GetMapping(value = "/node", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> navigate(@RequestParam MultiValueMap<String, String> requestParameters, WebSession session) {

        String id =  Objects.requireNonNull(requestParameters.getFirst("id"), "Parameter 'id' is required here. ");
        log.info("Request to navigate to node {}", id);
        if(! id.startsWith("/api"))  {
            URI location = UriComponentsBuilder
                    .fromUri(URI.create(id))
                    .build().toUri();

            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .location(location)
                    .build();

        } else {
            MultiValueMap<String,String> params = new LinkedMultiValueMap<>(requestParameters);
            String[] split = id.substring(5).split("/");
            String key = "";
            String val = "";

            for (int i = 0; i < split.length; i++) {
                if(i%2 == 0) {
                    if(StringUtils.hasLength(key) && StringUtils.hasLength(val)) {
                        params.set(key, val);
                        key = "";
                        val = "";
                    }
                    key = split[i];
                }
                else val = split[i];
            }
            if(StringUtils.hasLength(key) && ! StringUtils.hasLength(val)) params.set(key, "list");
            if(StringUtils.hasLength(key) && StringUtils.hasLength(val)) params.set(key, val);

            URI location = UriComponentsBuilder
                    .fromUri(URI.create("/nav/api"))
                    .queryParams(params)
                    .build().toUri();
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .location(location)
                    .build();
        }


    }


    @GetMapping(value = "/api", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<Statement> wrap(@RequestParam MultiValueMap<String, String> params, @RequestHeader Map<String, String> headers) {
        log.info("Request to navigate to params {}", params);


        return super.acquireContext()
                .doOnSubscribe(sub -> log.info("Request to navigate to start page"))
                .flatMapMany(context -> this.navigationServices.browse(new HashMap<>(params.toSingleValueMap()), context));
    }



}
