package org.av360.maverick.graph.feature.jobs;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.av360.maverick.graph.model.aspects.Job;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.services.ConfigurationService;
import org.av360.maverick.graph.services.EntityServices;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

@Job
@Slf4j(topic = "graph.feat.jobs.exports")
public class ExportRepositoryJob implements ScheduledJob {

    public static String NAME = "exportApplication";
    private final EntityServices entityServices;
    private final ConfigurationService configurationService;

    public static final String CONFIG_KEY_EXPORT_LOCAL_PATH = "export_local_path";
    public static final String CONFIG_KEY_EXPORT_S3_HOST = "export_s3_host";
    public static final String CONFIG_KEY_EXPORT_S3_BUCKET = "export_s3_bucket";

    public ExportRepositoryJob(EntityServices service,  ConfigurationService configurationService) {
        this.entityServices = service;
        this.configurationService = configurationService;
        ConfigurationKeysRegistry.add(CONFIG_KEY_EXPORT_LOCAL_PATH, "Local directory for exporting files.");
        ConfigurationKeysRegistry.add(CONFIG_KEY_EXPORT_S3_HOST, "Name of S3 host for exporting files.");
        ConfigurationKeysRegistry.add(CONFIG_KEY_EXPORT_S3_BUCKET, "Name of S3 bucket for exporting files.");
    }

    @Override
    public String getName() {
        return NAME;
    }

    protected String resolveLocalStorageDirectory(SessionContext ctx) {
        return configurationService.getValue(CONFIG_KEY_EXPORT_LOCAL_PATH, ctx).block();
    }
    protected String resolveS3Host(SessionContext ctx) {
        return configurationService.getValue(CONFIG_KEY_EXPORT_S3_HOST, ctx).block();
    }
    protected String resolveS3Bucket(SessionContext ctx) {
        return configurationService.getValue(CONFIG_KEY_EXPORT_S3_BUCKET, ctx).block();
    }

    @Override
    public Mono<Void> run(SessionContext ctx) {
        return ValidateReactive.notNull(ctx.getEnvironment().getRepositoryType())
                .then(this.compress(this.exportRdfStatements(ctx), ctx))
                .flatMap(zippedFilePath -> {
                    List<Mono<Void>> operations = new ArrayList<>();

                    if (StringUtils.hasLength(resolveS3Bucket(ctx))) {
                        S3AsyncClient s3Client = createS3Client(resolveS3Host(ctx));
                        operations.add(uploadRdfToS3(s3Client, zippedFilePath, ctx).then());
                    }

                    return Flux.merge(operations).then();
                });


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
                            RDFWriter writer = RDFWriterRegistry.getInstance().get(RDFFormat.NQUADS).get().getWriter(new OutputStreamWriter(out));
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

    private Mono<Path> compress(Flux<DataBuffer> dataBufferFlux, SessionContext ctx) {
        String filename = Objects.nonNull(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope().label() : "default";

        try {
            Path filePath = getLocalStorageDirectory(ctx).resolve("%s.nq".formatted(filename));
            Path zippedFilePath = getLocalStorageDirectory(ctx).resolve("%s.nq.gz".formatted(filename));
            return DataBufferUtils.write(dataBufferFlux, filePath)
                    .then(Mono.fromRunnable(() -> {

                        try (InputStream fis = Files.newInputStream(filePath);
                             OutputStream fos = Files.newOutputStream(zippedFilePath);
                             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)
                        ){
                            IOUtils.copy(fis, gzipOS);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .then(Mono.just(zippedFilePath));


        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    private Mono<Void> saveRdfToLocalPath(Flux<DataBuffer> dataBufferFlux, SessionContext ctx) {
        String filename = Objects.nonNull(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope().label() : "default";

        try {
            Path filePath = getLocalStorageDirectory(ctx).resolve("%s.nq".formatted(filename));
            return DataBufferUtils.write(dataBufferFlux, filePath).then();
        } catch (IOException e) {
            return Mono.error(e);
        }

    }


    private Path getLocalStorageDirectory(SessionContext ctx) throws IOException {
        Path directoryPath = Paths.get(this.resolveLocalStorageDirectory(ctx));
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            throw new IOException("Error creating directories for export", e);
        }

        return directoryPath;
    }


    private Mono<PutObjectResponse> uploadRdfToS3(S3AsyncClient s3Client, Path zippedFilePath, SessionContext ctx) {
        String filename = Objects.nonNull(ctx.getEnvironment().getScope()) ? ctx.getEnvironment().getScope().label() : "default";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveS3Bucket(ctx))
                .key("%s.nq.gz".formatted(filename))
                .build();

        DataBufferUtils.read(zippedFilePath, new DefaultDataBufferFactory(), 8192);

        Flux<ByteBuffer> buffers =
                DataBufferUtils.read(zippedFilePath, new DefaultDataBufferFactory(), 8192)
                        .flatMap(dataBuffer -> Flux.fromIterable(dataBuffer::readableByteBuffers));

        //FIXME Is this correct? https://stackoverflow.com/a/76388223
        // Flux<ByteBuffer> byteBuffers = dataBufferFlux.flatMap(dataBuffer -> Flux.fromIterable(dataBuffer::readableByteBuffers));


        return Mono.fromCompletionStage(s3Client.putObject(
                putObjectRequest,
                AsyncRequestBody.fromPublisher(buffers)
        ));
    }

}
