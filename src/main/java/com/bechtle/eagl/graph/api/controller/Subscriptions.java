package com.bechtle.eagl.graph.api.controller;

import com.bechtle.eagl.graph.api.controller.dto.admin.subscriptions.Requests;
import com.bechtle.eagl.graph.api.controller.dto.admin.subscriptions.Responses;
import com.bechtle.eagl.graph.domain.services.SubscriptionsService;
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
import java.util.Set;

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
                .create()
                .map(Responses.Subscription::new);
    }

    @ApiOperation(value = "List all subscriptions", tags = {"v2"})
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.Subscription> listSubscription() {
        log.info("(Request) List all subscriptions");
        return this.subscriptionsService
                .getSubscriptions()
                .map(Responses.Subscription::new);
    }

    @ApiOperation(value = "Generate API Key", tags = {"v2"})
    @PostMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.SubscriptionWithKey> generateKey(@PathVariable String subscriptionId, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.name()), "Name is a required parameter");

        log.info("(Request) Generate a new api key for subscription {} with name {}", subscriptionId, request.name());
        return this.subscriptionsService
                .generateApiKey(subscriptionId, request.name())
                .map(key -> new Responses.SubscriptionWithKey(subscriptionId, List.of(key)));

    }

    @ApiOperation(value = "List API keys", tags = {"v2"})
    @GetMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.SubscriptionWithKey> listKeys(@PathVariable String subscriptionId) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");

        log.info("(Request) List api keys for subscription {}", subscriptionId);
        return this.subscriptionsService
                .getKeysForSubscription(subscriptionId)
                .collectList()
                .map(keys -> new Responses.SubscriptionWithKey(subscriptionId, keys));

    }


    @ApiOperation(value = "Revoke API Key", tags = {"v2"})
    @DeleteMapping(value = "/{subscriptionId}/keys/{name}")
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
