package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.schedulers.ScopedScheduledExportApplication;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.feature.applications.services.model.Application;
import org.av360.maverick.graph.feature.applications.services.model.ApplicationFlags;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.EntityServices;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
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
import java.util.Map;

@Service
@Slf4j(topic = "graph.feat.jobs.exports")
public class ExportApplicationJob implements Job {

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


    public ExportApplicationJob(EntityServices service, ApplicationsService applicationsService) {
        this.entityServices = service;
        this.applicationsService = applicationsService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mono<Void> run(Authentication authentication) {
        return ReactiveApplicationContextHolder.getRequestedApplicationLabel()
                .flatMap(label -> applicationsService.getApplicationByLabel(label, authentication))
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
                        return exportRdfStatements(authentication)
                                .flatMap(rdfString -> saveRdfStringToLocalPath(rdfString, application)).then();
                    } else {
                        S3AsyncClient s3Client = createS3Client(application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_HOST).toString());
                        return exportRdfStatements(authentication)
                                .flatMap(rdfString -> uploadRdfStringToS3(s3Client, rdfString, application)).then();
                    }
                });
    }

    private S3AsyncClient createS3Client(String s3Host) {
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(s3Host))
                .region(Region.US_EAST_1)
                .build();
    }

    private Mono<String> exportRdfStatements(Authentication authentication) {
        return this.entityServices.getStore().listStatements(null, null, null, authentication)
                .map(statements -> {
                    StringWriter stringWriter = new StringWriter();
                    RDFWriter writer = RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).get().getWriter(stringWriter);
                    writer.startRDF();
                    statements.forEach(writer::handleStatement);
                    writer.endRDF();

                    return stringWriter.toString();
                });
    }

    private Mono<Void> saveRdfStringToLocalPath(String rdfString, Application application) {
        return Mono.fromCallable(() -> {
            Files.writeString(
                    Paths.get(
                            application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_LOCAL_PATH).toString(),
                            application.label() + ".txt"
                    ),
                    rdfString,
                    StandardCharsets.UTF_8);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<PutObjectResponse> uploadRdfStringToS3(S3AsyncClient s3Client, String rdfString, Application application) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(application.configuration().get(ScopedScheduledExportApplication.CONFIG_KEY_EXPORT_S3_BUCKET).toString())
                .key(application.label() + ".txt")
                .build();
        return Mono.fromCompletionStage(s3Client.putObject(
                putObjectRequest,
                AsyncRequestBody.fromBytes(rdfString.getBytes(StandardCharsets.UTF_8))
        ));
    }
}
