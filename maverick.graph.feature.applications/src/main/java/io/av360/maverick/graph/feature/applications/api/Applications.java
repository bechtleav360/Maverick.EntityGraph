package io.av360.maverick.graph.feature.applications.api;

import io.av360.maverick.graph.feature.applications.api.dto.Requests;
import io.av360.maverick.graph.feature.applications.api.dto.Responses;
import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import io.av360.maverick.graph.feature.applications.domain.errors.InvalidApplication;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
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
@RequestMapping(path = "/api/applications")
@Api(tags = "Manage applications")
@Slf4j(topic = "graph.feature.apps.api")
public class Applications extends AbstractController {

    private final ApplicationsService applicationsService;

    public Applications(ApplicationsService applicationsService) {

        this.applicationsService = applicationsService;
    }


    @ApiOperation(value = "Create a new application")
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

    @ApiOperation(value = "List all applications")
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

    @ApiOperation(value = "Generate API Key")
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

    @ApiOperation(value = "List registered API keys for application")
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

    @ApiOperation(value = "Revoke API Key")
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

    @ApiOperation(value = "Set application configuration")
    @PostMapping(value = "/{applicationId}/config")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> setApplicationConfig(@PathVariable String applicationId, @RequestBody Requests.SetApplicationConfigRequest request) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.s3Host()), "S3 Host is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.s3BucketId()), "S3 Bucket ID is a required parameter");
        Assert.isTrue(StringUtils.hasLength(request.exportFrequency()), "Export frequency (as Cron) is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication ->
                        this.applicationsService.setApplicationConfig(
                                applicationId,
                                request.s3Host(),
                                request.s3BucketId(),
                                request.exportFrequency(),
                                authentication))
                .then()
                .doOnSubscribe(subscription -> log.info("Setting application configuration"));
    }


    @ApiOperation(value = "Get application configuration")
    @GetMapping(value = "/{applicationId}/config")
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.ApplicationConfigResponse> getApplicationConfig(@PathVariable String applicationId) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.getApplicationConfig(applicationId, authentication))
                .map(config ->
                        new Responses.ApplicationConfigResponse(
                                config.label(),
                                config.persistent(),
                                config.s3Host(),
                                config.s3BucketId(),
                                config.exportFrequency()))
                .doOnSubscribe(subscription -> log.info("Fetching application configuration"));
    }

    @ApiOperation(value = "Export application")
    @PostMapping(value = "/{applicationId}/exports")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Responses.ExportResponse> exportApplication(@PathVariable String applicationId) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.exportApplication(applicationId, authentication))
                .map(export ->
                        new Responses.ExportResponse(export.id())
                ).doOnSubscribe(subscription -> log.info("Exporting an application"));
    }

    @ApiOperation(value = "Get export")
    @GetMapping(value = "/{applicationId}/exports/{exportId}")
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.GetExportResponse> getExport(@PathVariable String applicationId, @PathVariable String exportId) {
        Assert.isTrue(StringUtils.hasLength(applicationId), "Application ID is a required parameter");
        Assert.isTrue(StringUtils.hasLength(exportId), "Export ID is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.getExport(applicationId, exportId, authentication))
                .map(export ->
                        new Responses.GetExportResponse(
                                export.id(),
                                export.s3Host(),
                                export.s3BucketId(),
                                export.s3ObjectId()
                        )
                ).doOnSubscribe(subscription -> log.info("Fetching an export"));
    }
}
