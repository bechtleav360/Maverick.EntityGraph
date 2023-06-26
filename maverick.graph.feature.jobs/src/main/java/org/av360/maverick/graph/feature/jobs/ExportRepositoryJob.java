package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.services.ConfigurationService;
import org.av360.maverick.graph.services.EntityServices;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j(topic = "graph.feat.jobs.exports")
public class ExportRepositoryJob implements Job {

    public static String NAME = "exportApplication";
    private final EntityServices entityServices;
    private final ConfigurationService configurationService;

    public ExportRepositoryJob(EntityServices service,  ConfigurationService configurationService) {
        this.entityServices = service;
        this.configurationService = configurationService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    protected String resolveLocalStorageDirectory(SessionContext ctx) {
        // FIXME: @mumi, we don't want a dependency to the application feature here... the jobs module should also work if the applications module is inactive.
        // Solution: create a ConfigurationService (interface in model), which has a default implementation and a decorator in the applications module
        return configurationService.getValue("export_local_path", ctx).block();
    }
    protected String resolveS3Host(SessionContext ctx) {
        return configurationService.getValue("export_s3_host", ctx).block();
    }
    protected String resolveS3Bucket(SessionContext ctx) {
        return configurationService.getValue("export_s3_bucket", ctx).block();
    }

    @Override
    public Mono<Void> run(SessionContext ctx) {
        return ValidateReactive.notNull(ctx.getEnvironment().getRepositoryType())
                .then(Mono.defer(() -> {
                    // FIXME: @mumi, it should be valid to run address multiple export targets in parallel
                    // see also https://projectreactor.io/docs/core/release/api/reactor/core/publisher/MonoSink.html#success--
                    Flux<DataBuffer> dataBufferFlux = exportRdfStatements(ctx).share();

                    List<Mono<Void>> operations = new ArrayList<>();

                    if (StringUtils.hasLength(resolveLocalStorageDirectory(ctx))) {
                        operations.add(saveRdfToLocalPath(dataBufferFlux, ctx));
                    }

                    if (StringUtils.hasLength(resolveS3Bucket(ctx))) {
                        S3AsyncClient s3Client = createS3Client(resolveS3Host(ctx));
                        operations.add(uploadRdfToS3(s3Client, dataBufferFlux, ctx).then());
                    }

                    return Flux.merge(operations).then();
                }));
    }

    private S3AsyncClient createS3Client(String s3Host) {
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(s3Host))
                .region(Region.US_EAST_1)
                .build();
    }

    private Flux<DataBuffer> exportRdfStatements(SessionContext ctx) {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out;

        try {
            out = new PipedOutputStream(in);
        } catch (IOException e) {
            return Flux.error(e);
        }

        Mono.fromRunnable(() -> {
            try {
                this.entityServices.getStore(ctx).listStatements(null, null, null, ctx.getEnvironment())
                        .doOnNext(statements -> {
                            RDFWriter writer = RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).get().getWriter(new OutputStreamWriter(out));
                            writer.startRDF();
                            statements.forEach(writer::handleStatement);
                            writer.endRDF();
                        })
                        .publishOn(Schedulers.boundedElastic())
                        .doOnTerminate(() -> {
                            try {
                                out.close();
                            } catch (IOException e) {
                                log.error("Error closing stream", e);
                            }
                        })
                        .subscribe();
            } catch (Exception e) {
                log.error("Error exporting RDF statements", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();

        return DataBufferUtils.readInputStream(() -> in, new DefaultDataBufferFactory(), 8192);
    }

    private Mono<Void> saveRdfToLocalPath(Flux<DataBuffer> dataBufferFlux, SessionContext ctx) {
        String filename = Objects.nonNull(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope().label() : "default";

        return DataBufferUtils.write(
                dataBufferFlux,
                Paths.get(
                        this.resolveLocalStorageDirectory(ctx),
                        "%s.ttl".formatted(filename)
                )
        ).then();
    }

    private Mono<PutObjectResponse> uploadRdfToS3(S3AsyncClient s3Client, Flux<DataBuffer> dataBufferFlux, SessionContext ctx) {
        String filename = Objects.nonNull(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope().label() : "default";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveS3Bucket(ctx))
                .key("%s.ttl".formatted(filename))
                .build();

        //FIXME Is this correct? https://stackoverflow.com/a/76388223
        Flux<ByteBuffer> byteBuffers = dataBufferFlux.flatMap(dataBuffer -> Flux.fromIterable(dataBuffer::readableByteBuffers));

        return Mono.fromCompletionStage(s3Client.putObject(
                putObjectRequest,
                AsyncRequestBody.fromPublisher(byteBuffers)
        ));
    }

}
