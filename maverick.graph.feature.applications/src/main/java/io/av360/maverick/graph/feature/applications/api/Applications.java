package io.av360.maverick.graph.feature.applications.api;

import io.av360.maverick.graph.feature.applications.api.dto.Requests;
import io.av360.maverick.graph.feature.applications.api.dto.Responses;
import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import io.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(path = "/api/applications")
//@Api(tags = "Manage applications")
@Slf4j(topic = "graph.feature.apps.api")
@SecurityRequirement(name = "api_key")
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
                .flatMap(authentication -> this.applicationsService.createApplication(request.label(), request.persistent(), authentication))
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.persistent()
                        )
                ).doOnSubscribe(subscription -> log.info("Creating a new application"));
    }

    //@ApiOperation(value = "List all applications")
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.ApplicationResponse> listApplications() {
        return super.getAuthentication()
                .flatMapMany(this.applicationsService::getApplications)
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.persistent()
                        )
                ).doOnSubscribe(subscription -> log.info("Fetching all applications"));
    }


    //@ApiOperation(value = "Generate API Key")
    @PostMapping(value = "/{applicationId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApiKeyWithApplicationResponse> generateKey(@PathVariable String applicationId, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID  is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.label()), "Name is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.generateApiKey(applicationId, request.label(), authentication))
                .map(apiKey ->
                        new Responses.ApiKeyWithApplicationResponse(
                                apiKey.key(),
                                apiKey.issueDate(),
                                apiKey.active(),
                                new Responses.ApplicationResponse(
                                        apiKey.application().key(),
                                        apiKey.application().label(),
                                        apiKey.application().persistent()
                                )
                        )
                ).doOnSubscribe(subscription -> log.info("Generating a new api key for an application"));

    }

    //@ApiOperation(value = "List registered API keys for application")
    @GetMapping(value = "/{applicationId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationWithApiKeys> listKeys(@PathVariable String applicationId) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.applicationsService.getKeysForApplication(applicationId, authentication))
                .switchIfEmpty(Mono.error(new InvalidApplication(applicationId)))
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) return Mono.empty();

                    Application subscription = keys.get(0).application();
                    List<Responses.ApiKeyResponse> apiKeys = keys.stream().map(apiKey -> new Responses.ApiKeyResponse(apiKey.key(), apiKey.issueDate(), apiKey.active())).toList();
                    return Mono.just(new Responses.ApplicationWithApiKeys(subscription.key(), subscription.label(), subscription.persistent(), apiKeys));
                }).doOnSubscribe(s -> log.info("Fetching all api keys for an application"));


    }


    // @ApiOperation(value = "Revoke API Key")
    @DeleteMapping(value = "/{applicationId}/keys/{label}")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<Void> revokeToken(@PathVariable String applicationId, @PathVariable String label) {

        Assert.isTrue(StringUtils.hasLength(applicationId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(label), "Name is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.applicationsService.revokeApiKey(applicationId, label, authentication))
                .then()
                .doOnSubscribe(subscription -> log.info("Generating a new token for an application"));
    }
}
