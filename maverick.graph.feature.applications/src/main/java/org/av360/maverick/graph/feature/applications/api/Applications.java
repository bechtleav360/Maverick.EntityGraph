package org.av360.maverick.graph.feature.applications.api;

import org.av360.maverick.graph.feature.applications.api.dto.Requests;
import org.av360.maverick.graph.feature.applications.api.dto.Responses;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
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

    public Applications(ApplicationsService applicationsService) {

        this.applicationsService = applicationsService;
    }


    //@ApiOperation(value = "Create a new application")
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationResponse> createApplication(@RequestBody Requests.RegisterApplicationRequest request) {
        Assert.notNull(request.label(), "Label must be set in request");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.createApplication(request.label(), request.flags(), authentication))
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.flags()
                        )
                ).doOnSubscribe(subscription -> log.info("Request to create a new application with the label '{}' and flags: {}", request.label(), request.flags()));
    }

    //@ApiOperation(value = "List all applications")
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.ApplicationResponse> listApplications() {
        return super.getAuthentication()
                .flatMapMany(this.applicationsService::listApplications)
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.flags()
                        )
                ).doOnSubscribe(subscription -> log.info("Request to list all applications"));
    }

    @GetMapping(value = "/{applicationId}")
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.ApplicationResponse> getApplication(@PathVariable String applicationId) {
        return super.getAuthentication()
                .flatMap(auth -> this.applicationsService.getApplication(applicationId, auth))
                .map(application ->
                        new Responses.ApplicationResponse(
                                application.key(),
                                application.label(),
                                application.flags()
                        )
                ).doOnSubscribe(subscription -> log.info("Request to get application with id '{}'", applicationId));
    }


    //@ApiOperation(value = "Generate API Key")
    @PostMapping(value = "/{applicationId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApiKeyWithApplicationResponse> generateKey(@PathVariable String applicationId, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID  is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.label()), "Name is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.createSubscription(applicationId, request.label(), authentication))
                .map(apiKey ->
                        new Responses.ApiKeyWithApplicationResponse(
                                apiKey.key(),
                                apiKey.issueDate(),
                                apiKey.active(),
                                new Responses.ApplicationResponse(
                                        apiKey.application().key(),
                                        apiKey.application().label(),
                                        apiKey.application().flags()
                                )
                        )
                ).doOnSubscribe(subscription -> log.info("Generating a new api key for an application"));

    }

    //@ApiOperation(value = "List registered API keys for application")
    @GetMapping(value = "/{applicationId}/subscriptions")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.SubscriptionResponse> listSubscriptions(@PathVariable String applicationId) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.applicationsService.listSubscriptionsForApplication(applicationId, authentication))
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationId)))
                .map(subscription -> new Responses.SubscriptionResponse(subscription.key(), subscription.issueDate(), subscription.active()))
                .doOnSubscribe(s -> log.info("Fetching all api keys for an application"));


    }


    // @ApiOperation(value = "Revoke API Key")
    @DeleteMapping(value = "/{applicationId}/subscriptions/{label}")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<Void> revokeToken(@PathVariable String applicationId, @PathVariable String label) {

        Assert.isTrue(StringUtils.hasLength(applicationId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(label), "Name is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.applicationsService.revokeToken(applicationId, label, authentication))
                .then()
                .doOnSubscribe(subscription -> log.info("Generating a new token for an application"));
    }
}