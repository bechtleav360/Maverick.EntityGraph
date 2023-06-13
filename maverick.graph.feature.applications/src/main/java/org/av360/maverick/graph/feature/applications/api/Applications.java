package org.av360.maverick.graph.feature.applications.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.applications.api.dto.Requests;
import org.av360.maverick.graph.feature.applications.api.dto.Responses;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.SubscriptionsService;
import org.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.RepositoryType;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/applications")
//@Api(tags = "Manage applications")
@Slf4j(topic = "graph.feat.apps.ctrl.api")
@SecurityRequirement(name = "api_key")
@Tag(name = "Manage applications")
public class Applications extends AbstractController {

    private final ApplicationsService applicationsService;

    private final SubscriptionsService subscriptionsService;

    private final QueryServices queryServices;

    public Applications(ApplicationsService applicationsService, SubscriptionsService subscriptionsService, QueryServices queryServices) {

        this.applicationsService = applicationsService;
        this.subscriptionsService = subscriptionsService;
        this.queryServices = queryServices;
    }


    //@ApiOperation(value = "Create a new node")
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationResponse> createApplication(@RequestBody Requests.RegisterApplicationRequest request) {
        Assert.notNull(request.label(), "Label must be set in request");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.createApplication(request.label(), request.flags(), request.configuration(), authentication))
                .map(application ->
                        new Responses.ApplicationResponse(
                                application.key(),
                                application.label(),
                                application.flags(),
                                application.configuration()
                        )
                ).doOnSubscribe(subscription -> log.info("Request to create a new node with the label '{}' and flags: {}", request.label(), request.flags()));
    }

    //@ApiOperation(value = "List all applications")
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.ApplicationResponse> listApplications() {
        return super.getAuthentication()
                .flatMapMany(this.applicationsService::listApplications)
                .map(application ->
                        new Responses.ApplicationResponse(
                                application.key(),
                                application.label(),
                                application.flags(),
                                application.configuration()
                        )
                ).doOnSubscribe(subscription -> log.info("Request to list all applications"));
    }

    @GetMapping(value = "/{applicationKey}")
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.ApplicationResponse> getApplication(@PathVariable String applicationKey) {
        return super.getAuthentication()
                .flatMap(auth -> this.applicationsService.getApplication(applicationKey, auth))
                .map(application ->
                        new Responses.ApplicationResponse(
                                application.key(),
                                application.label(),
                                application.flags(),
                                application.configuration()
                        )
                ).doOnSubscribe(subscription -> log.info("Request to get application with id '{}'", applicationKey));
    }


    @PostMapping(value = "/{applicationKey}/configuration/{configurationKey}")
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.ApplicationResponse> createConfiguration(@PathVariable String applicationKey, @PathVariable String configurationKey, @RequestBody String value) {
        return super.getAuthentication()
                .flatMap(auth ->
                        this.applicationsService.getApplication(applicationKey, auth)
                                .flatMap(application -> this.applicationsService.createConfigurationItem(application, configurationKey, value, auth)))
                .map(application ->
                        new Responses.ApplicationResponse(
                                application.key(),
                                application.label(),
                                application.flags(),
                                application.configuration()
                        )

                ).doOnSubscribe(subscription -> log.info("Request to update configuration key '{}' for application with id '{}'", configurationKey, applicationKey));
    }

    @DeleteMapping(value = "/{applicationKey}/configuration/{configurationKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Responses.ApplicationResponse> deleteConfiguration(@PathVariable String applicationKey, @PathVariable String configurationKey) {
        return super.getAuthentication()
                .flatMap(auth ->
                        this.applicationsService.getApplication(applicationKey, auth)
                                .flatMap(application -> this.applicationsService.deleteConfigurationItem(application, configurationKey, auth)))
                .then(this.getApplication(applicationKey))
                .doOnSubscribe(subscription -> log.info("Request to delete configuration key '{}' for application with id '{}'", configurationKey, applicationKey));
    }

    @DeleteMapping(value = "/{applicationKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> deleteApplication(@PathVariable String applicationKey) {
        return super.getAuthentication()
                .flatMap(auth ->
                        this.applicationsService.getApplication(applicationKey, auth)
                                .flatMap(application -> this.applicationsService.delete(application, auth)))
                .doOnSubscribe(subscription -> log.info("Request to delete application with id '{}'", applicationKey));
    }


    //@ApiOperation(value = "Generate API Key")
    @PostMapping(value = "/{applicationKey}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApiKeyWithApplicationResponse> generateKey(@PathVariable String applicationKey, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(applicationKey), "Application Key  is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.label()), "Name is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.subscriptionsService.createSubscription(applicationKey, request.label(), authentication))
                .map(apiKey ->
                        new Responses.ApiKeyWithApplicationResponse(
                                apiKey.key(),
                                apiKey.issueDate(),
                                apiKey.active(),
                                new Responses.ApplicationResponse(
                                        apiKey.application().key(),
                                        apiKey.application().label(),
                                        apiKey.application().flags(),
                                        apiKey.application().configuration()
                                )
                        )
                ).doOnSubscribe(subscription -> log.info("Request to generate a new api key for an node"));

    }

    //@ApiOperation(value = "List registered API keys for node")
    @GetMapping(value = "/{applicationKey}/subscriptions")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.SubscriptionResponse> listSubscriptions(@PathVariable String applicationKey) {
        Assert.isTrue(StringUtils.hasLength(applicationKey), "Application Key is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.subscriptionsService.listSubscriptionsForApplication(applicationKey, authentication))
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationKey)))
                .map(subscription -> new Responses.SubscriptionResponse(subscription.key(), subscription.issueDate(), subscription.active()))
                .doOnSubscribe(s -> log.info("Fetching all api keys for an node"));


    }


    // @ApiOperation(value = "Revoke API Key")
    @DeleteMapping(value = "/{applicationKey}/subscriptions/{label}")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<Void> revokeToken(@PathVariable String applicationKey, @PathVariable String label) {

        Assert.isTrue(StringUtils.hasLength(applicationKey), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(label), "Name is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.subscriptionsService.revokeToken(applicationKey, label, authentication))
                .then()
                .doOnSubscribe(subscription -> log.info("Generating a new token for an node"));
    }


    @PostMapping(value = "/query", consumes = "text/plain", produces = {"text/turtle", "node/ld+json"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Sparql Construct Query",
            content = @Content(examples = {
                    @ExampleObject(name = "Query everything", value = "CONSTRUCT WHERE { ?s ?p ?o . } LIMIT 100")
            })
    )
    Flux<AnnotatedStatement> query(@RequestBody String query) {
        return getAuthentication()
                .flatMapMany(authentication -> queryServices.queryGraph(query, authentication, RepositoryType.APPLICATION))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to dump node graph");
                });
    }



}
