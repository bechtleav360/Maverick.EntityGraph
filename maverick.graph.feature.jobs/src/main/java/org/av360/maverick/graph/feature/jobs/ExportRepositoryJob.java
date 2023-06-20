package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.services.EntityServices;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Slf4j(topic = "graph.feat.jobs.exports")
public class ExportRepositoryJob implements Job {

    public static String NAME = "exportApplication";
    private final EntityServices entityServices;

    private final ApplicationsService applicationsService;

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultExportFrequency:}")
    private String defaultExportFrequency;

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultLocalPath:}")
    private String defaultLocalPath;

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultS3Host:}")
    private String defaultS3Host;

    @Value("${application.features.modules.jobs.scheduled.exportApplication.defaultS3BucketId:}")
    private String defaultS3BucketId;


    public ExportRepositoryJob(EntityServices service, ApplicationsService applicationsService) {
        this.entityServices = service;
        this.applicationsService = applicationsService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mono<Void> run(SessionContext ctx) {
        return ValidateReactive.notNull(ctx.getEnvironment().getRepositoryType())
                .then(Mono.<Void>create(sink -> {
                    // FIXME: @mumi, it should be valid to run address multiple export targets in parallel
                    // see also https://projectreactor.io/docs/core/release/api/reactor/core/publisher/MonoSink.html#success--
                    if (StringUtils.hasLength(resolveLocalStorageDirectory())) {
                        try {

                            exportRdfStatements(ctx)
                                    .flatMap(rdfString -> saveRdfStringToLocalPath(rdfString, ctx))
                                    .doOnSuccess(s -> sink.success());

                        } catch (Exception e) {
                            sink.error(e);
                        }
                    }

                    if (StringUtils.hasLength(resolveS3Bucket())) {
                        try (S3AsyncClient s3Client = createS3Client(resolveS3Host())) {


                            exportRdfStatements(ctx)
                                    .flatMap(rdfString -> uploadRdfStringToS3(s3Client, rdfString, ctx))
                                    .doOnSuccess(s -> sink.success());
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    }

                }));

    /*
        return ReactiveApplicationContextHolder.getRequestedApplicationLabel()
                .flatMap(label -> applicationsService.getApplicationByLabel(label, ctx))
                .defaultIfEmpty(new Application(
                        SimpleValueFactory.getInstance().createIRI(Local.Applications.NAMESPACE, "default"),
                        Globals.DEFAULT_APPLICATION_LABEL,
                        "default",
                        new ApplicationFlags(true, true),
                        Map.of(
                                ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_FREQUENCY, defaultExportFrequency,
                                ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_LOCAL_PATH, defaultLocalPath,
                                ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_BUCKET, defaultS3BucketId,
                                ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_HOST, defaultS3Host
                        )
                ))
                .flatMap(application -> {
                    if (defaultLocalPath != null && !defaultLocalPath.isEmpty()) {
                        return exportRdfStatements(ctx)
                                .flatMap(rdfString -> saveRdfStringToLocalPath(rdfString, application)).then();
                    } else {
                        S3AsyncClient s3Client = createS3Client(application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_HOST).toString());
                        return exportRdfStatements(ctx)
                                .flatMap(rdfString -> uploadRdfStringToS3(s3Client, rdfString, application)).then();
                    }
                });
                */
    }

    private S3AsyncClient createS3Client(String s3Host) {
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(s3Host))
                .region(Region.US_EAST_1)
                .build();
    }

    // TODO: @mumi, Don't return a Mono<String>, but a streaming Flux<DataBuffer> ... you never know how a large the dumps will be, and you don't want to run out of memory
    // You can use a pipedstream to forward the stream either into a file or into an s3 endpoint (check DataBufferUtils)
    private Mono<String> exportRdfStatements(SessionContext ctx) {
        return this.entityServices.getStore(ctx).listStatements(null, null, null, ctx.getEnvironment())
                .map(statements -> {
                    StringWriter stringWriter = new StringWriter();
                    RDFWriter writer = RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).get().getWriter(stringWriter);
                    writer.startRDF();
                    statements.forEach(writer::handleStatement);
                    writer.endRDF();

                    return stringWriter.toString();
                });
    }

    private Mono<Void> saveRdfStringToLocalPath(String rdfString, SessionContext ctx) {
        return Mono.fromCallable(() -> {
            String filename = StringUtils.hasLength(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope() : "default";
            // FIXME: @mumi, write stream into file
            Files.writeString(
                    Paths.get(
                            this.resolveLocalStorageDirectory(),
                            "%s.ttl".formatted(filename)
                    ),
                    rdfString,
                    StandardCharsets.UTF_8);
            return null;
            // FIXME: @mumi, are you sure about the subscribeOn here?
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    protected String resolveLocalStorageDirectory() {
        // FIXME: @mumi, we don't want a dependency to the application feature here... the jobs module should also work if the applications module is inactive.
        // Solution: create a ConfigurationService (interface in model), which has a default implementation and a decorator in the applications module
        // application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_LOCAL_PATH).toString(),
        return defaultLocalPath;
    }

    protected String resolveS3Bucket() {
        // application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_BUCKET).toString()
        return defaultS3BucketId;
    }

    protected String resolveS3Host() {
        // application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_HOST).toString()
        return defaultS3Host;
    }

    private Mono<PutObjectResponse> uploadRdfStringToS3(S3AsyncClient s3Client, String rdfString, SessionContext ctx) {
        String filename = StringUtils.hasLength(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope() : "default";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveS3Bucket())
                .key("%s.ttl".formatted(filename))
                .build();
        return Mono.fromCompletionStage(s3Client.putObject(
                putObjectRequest,
                AsyncRequestBody.fromBytes(rdfString.getBytes(StandardCharsets.UTF_8))
        ));
    }
}
