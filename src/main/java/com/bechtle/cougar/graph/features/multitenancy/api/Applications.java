package com.bechtle.cougar.graph.features.multitenancy.api;

import com.bechtle.cougar.graph.features.multitenancy.api.dto.Requests;
import com.bechtle.cougar.graph.features.multitenancy.api.dto.Responses;
import com.bechtle.cougar.graph.features.multitenancy.domain.ApplicationsService;
import com.bechtle.cougar.graph.features.multitenancy.domain.errors.InvalidApplication;
import com.bechtle.cougar.graph.features.multitenancy.domain.model.Application;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(path = "/api/admin/subscriptions")
@Api(tags = "Admin")
@Slf4j(topic = "cougar.graph.feature.subscriptions")
public class Applications {

    private final ApplicationsService subscriptionsService;

    public Applications(ApplicationsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }


    @ApiOperation(value = "Create a new subscription")
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationResponse> createSubscription(@RequestBody Requests.RegisterApplicationRequest request) {
        Assert.notNull(request.label(), "Label must be set in request");

        log.info("(Request) Create a new subscription with label {} and persistence {}", request.label(), request.persistent());

        return this.subscriptionsService
                .createSubscription(request.label(), request.persistent())
                .map(subscription ->
                    new Responses.ApplicationResponse(
                            subscription.key(),
                            subscription.label(),
                            subscription.persistent()
                    )
                );
    }


    @ApiOperation(value = "List all subscriptions", tags = {"v2"})
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.ApplicationResponse> listSubscription() {
        log.info("(Request) List all subscriptions");
        return this.subscriptionsService
                .getSubscriptions()
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.persistent()
                        )
                );
    }

    @ApiOperation(value = "Generate API Key", tags = {"v2"})
    @PostMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApiKeyWithApplicationResponse> generateKey(@PathVariable String subscriptionId, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.label()), "Name is a required parameter");

        log.info("(Request) Generate a new api key for subscription {} with label {}", subscriptionId, request.label());
        return this.subscriptionsService
                .generateApiKey(subscriptionId, request.label())
                .map(apiKey ->
                    new Responses.ApiKeyWithApplicationResponse(
                            apiKey.key(),
                            apiKey.issueDate(),
                            apiKey.active(),
                            new Responses.ApplicationResponse(
                                    apiKey.subscription().key(),
                                    apiKey.subscription().label(),
                                    apiKey.subscription().persistent()
                            )
                    )
                );

    }

    @ApiOperation(value = "List API keys", tags = {"v2"})
    @GetMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationWithApiKeys> listKeys(@PathVariable String subscriptionId) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");

        log.info("(Request) List api keys for subscription {}", subscriptionId);
        return this.subscriptionsService
                .getKeysForSubscription(subscriptionId)
                .switchIfEmpty(Mono.error(new InvalidApplication(subscriptionId)))
                .collectList()
                .flatMap(keys -> {
                    if(keys.isEmpty()) return Mono.empty();

                    Application subscription = keys.get(0).subscription();
                    List<Responses.ApiKeyResponse> apiKeys = keys.stream().map(apiKey -> new Responses.ApiKeyResponse(apiKey.key(), apiKey.issueDate(), apiKey.active())).toList();
                    return Mono.just(new Responses.ApplicationWithApiKeys(subscription.key(), subscription.label(), subscription.persistent(), apiKeys));
                });

    }


    @ApiOperation(value = "Revoke API Key", tags = {"v2"})
    @DeleteMapping(value = "/{subscriptionId}/keys/{label}")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<Void> revokeToken(@PathVariable String subscriptionId, @PathVariable String name) {
        log.info("(Request) Generate a new token for subscription {}", subscriptionId);
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(name), "Name is a required parameter");

        return this.subscriptionsService
                .revokeApiKey(subscriptionId, name)
                .then();
    }
}
