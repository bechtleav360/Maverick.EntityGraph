package cougar.graph.feature.applications.api;

import cougar.graph.feature.applications.api.dto.Requests;
import cougar.graph.feature.applications.api.dto.Responses;
import cougar.graph.api.controller.AbstractController;
import cougar.graph.feature.applications.domain.ApplicationsService;
import cougar.graph.feature.applications.domain.errors.InvalidApplication;
import cougar.graph.feature.applications.domain.model.Application;
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
@RequestMapping(path = "/api/subscriptions")
@Slf4j(topic = "graph.feature.apps.api")
public class Applications extends AbstractController {

    private final ApplicationsService subscriptionsService;

    public Applications(ApplicationsService subscriptionsService) {

        this.subscriptionsService = subscriptionsService;
    }


    @ApiOperation(value = "Create a new application")
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationResponse> createSubscription(@RequestBody Requests.RegisterApplicationRequest request) {
        Assert.notNull(request.label(), "Label must be set in request");

        return super.getAuthentication()
                .flatMap(authentication -> this.subscriptionsService.createSubscription(request.label(), request.persistent(), authentication))
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.persistent()
                        )
                ).doOnSubscribe(subscription -> log.info("Creating a new application"));
    }


    @ApiOperation(value = "List all subscriptions")
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.ApplicationResponse> listSubscription() {
        return super.getAuthentication()
                .flatMapMany(this.subscriptionsService::getSubscriptions)
                .map(subscription ->
                        new Responses.ApplicationResponse(
                                subscription.key(),
                                subscription.label(),
                                subscription.persistent()
                        )
                ).doOnSubscribe(subscription -> log.info("Fetching all subscriptions"));
    }

    @ApiOperation(value = "Generate API Key")
    @PostMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApiKeyWithApplicationResponse> generateKey(@PathVariable String subscriptionId, @RequestBody Requests.CreateApiKeyRequest request) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.label()), "Name is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.subscriptionsService.generateApiKey(subscriptionId, request.label(), authentication))
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

    @ApiOperation(value = "List API keys")
    @GetMapping(value = "/{subscriptionId}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Responses.ApplicationWithApiKeys> listKeys(@PathVariable String subscriptionId) {
        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.subscriptionsService.getKeysForSubscription(subscriptionId, authentication))
                .switchIfEmpty(Mono.error(new InvalidApplication(subscriptionId)))
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) return Mono.empty();

                    Application subscription = keys.get(0).application();
                    List<Responses.ApiKeyResponse> apiKeys = keys.stream().map(apiKey -> new Responses.ApiKeyResponse(apiKey.key(), apiKey.issueDate(), apiKey.active())).toList();
                    return Mono.just(new Responses.ApplicationWithApiKeys(subscription.key(), subscription.label(), subscription.persistent(), apiKeys));
                }).doOnSubscribe(s -> log.info("Fetching all api keys for an application"));


    }


    @ApiOperation(value = "Revoke API Key")
    @DeleteMapping(value = "/{subscriptionId}/keys/{label}")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<Void> revokeToken(@PathVariable String subscriptionId, @PathVariable String name) {

        Assert.isTrue(StringUtils.hasLength(subscriptionId), "Subscription is a required parameter");
        Assert.isTrue(StringUtils.hasLength(name), "Name is a required parameter");

        return super.getAuthentication()
                .flatMapMany(authentication -> this.subscriptionsService.revokeApiKey(subscriptionId, name, authentication))
                .then()
                .doOnSubscribe(subscription -> log.info("Generating a new token for an application"));
    }
}
