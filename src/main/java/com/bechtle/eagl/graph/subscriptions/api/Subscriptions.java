package com.bechtle.eagl.graph.subscriptions.api;

import com.bechtle.eagl.graph.subscriptions.domain.SubscriptionsService;
import com.bechtle.eagl.graph.subscriptions.api.dto.Requests;
import com.bechtle.eagl.graph.subscriptions.api.dto.Responses;
import com.bechtle.eagl.graph.subscriptions.domain.errors.InvalidSubscription;
import com.bechtle.eagl.graph.subscriptions.domain.model.Subscription;
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
@Slf4j
public class Subscriptions {

    private final SubscriptionsService subscriptionsService;

    public Subscriptions(SubscriptionsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }


    @ApiOperation(value = "Create a new subscription", tags = {"v2"})
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.Subscription> createSubscription(Requests.CreateSubscriptionRequest request) {
        log.info("(Request) Create a new subscription");
        return this.subscriptionsService
                .createSubscription(request.label(), request.persistent())
                .map(subscription ->
                    new Responses.Subscription(
                            subscription.key(),
                            subscription.label(),
                            subscription.persistent()
                    )
                );
    }

    @ApiOperation(value = "List all subscriptions", tags = {"v2"})
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.Subscription> listSubscription() {
        log.info("(Request) List all subscriptions");
        return this.subscriptionsService
                .getSubscriptions()
                .map(subscription ->
                        new Responses.Subscription(
                                subscription.key(),
                                subscription.label(),
                                subscription.persistent()
                        )
                );
    }

    @ApiOperation(value = "Generate API Key", tags = {"v2"})
    @PostMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApiKeyWithSubscription> generateKey(@PathVariable String subscriptionId, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.label()), "Name is a required parameter");

        log.info("(Request) Generate a new api key for subscription {} with label {}", subscriptionId, request.label());
        return this.subscriptionsService
                .generateApiKey(subscriptionId, request.label())
                .map(apiKey ->
                    new Responses.ApiKeyWithSubscription(
                            apiKey.key(),
                            apiKey.issueDate(),
                            apiKey.active(),
                            new Responses.Subscription(
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
    Mono<Responses.SubscriptionWithKeys> listKeys(@PathVariable String subscriptionId) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");

        log.info("(Request) List api keys for subscription {}", subscriptionId);
        return this.subscriptionsService
                .getKeysForSubscription(subscriptionId)
                .switchIfEmpty(Mono.error(new InvalidSubscription(subscriptionId)))
                .collectList()
                .flatMap(keys -> {
                    if(keys.isEmpty()) return Mono.empty();

                    Subscription subscription = keys.get(0).subscription();
                    List<Responses.ApiKey> apiKeys = keys.stream().map(apiKey -> new Responses.ApiKey(apiKey.key(), apiKey.issueDate(), apiKey.active())).toList();
                    return Mono.just(new Responses.SubscriptionWithKeys(subscription.key(), subscription.label(), subscription.persistent(), apiKeys));
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
