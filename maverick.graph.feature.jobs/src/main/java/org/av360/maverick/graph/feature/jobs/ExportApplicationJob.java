package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.services.EntityServices;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j(topic = "graph.jobs.export")
public class ExportApplicationJob implements Job {

    public static String NAME = "exportApplication";
    private final EntityServices entityServices;
    public ExportApplicationJob(EntityServices service) {
        this.entityServices = service;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mono<Void> run(Authentication authentication) {
        return ReactiveApplicationContextHolder.getRequestedApplication()
                .flatMap(application -> {
                    S3AsyncClient s3Client = S3AsyncClient.builder()
                            .endpointOverride(URI.create("http://127.0.0.1:9000"))
                            .region(Region.US_EAST_1)
                            .build();

                    return this.entityServices.getStore().listStatements(null, null, null, authentication)
                            .map(statements -> {
                                StringWriter stringWriter = new StringWriter();
                                RDFWriter writer = RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).get().getWriter(stringWriter);
                                writer.startRDF();
                                statements.forEach(writer::handleStatement);
                                writer.endRDF();

                                return stringWriter.toString();
                            })
                            .flatMap(rdfString -> {
                                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                        .bucket(application.flags().s3BucketId())
                                        .key(application.label() + ".txt")
                                        .build();
                                return Mono.fromCompletionStage(s3Client.putObject(
                                        putObjectRequest,
                                        AsyncRequestBody.fromBytes(rdfString.getBytes(StandardCharsets.UTF_8))
                                ));
                            });
                }).then();
    }
}
