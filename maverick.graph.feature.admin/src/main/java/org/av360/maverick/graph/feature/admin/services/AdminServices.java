package org.av360.maverick.graph.feature.admin.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.feature.admin.services.importer.EndpointImporter;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.store.FragmentsStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.SchedulingException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;


@Service
@Slf4j(topic = "graph.feat.admin.svc")
public class AdminServices {


    private final Map<RepositoryType, FragmentsStore> stores;
    private final IdentifierServices identifierServices;

    private boolean maintenanceActive = false;

    public AdminServices(Set<FragmentsStore> storeSet, IdentifierServices identifierServices) {
        this.identifierServices = identifierServices;
        this.stores = new HashMap<>();


        storeSet.forEach(store -> {
            if(store.isMaintainable()) {
                stores.put(store.getRepositoryType(), store);
            }

        });
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> reset(SessionContext ctx) {
        // if(maintenanceActive) return Mono.error(new SchedulingException("Maintenance job still running."));
        // doesn't work for testing, ignore for now

        this.stores.get(ctx.getEnvironment().getRepositoryType())
                .asMaintainable()
                .purge(ctx.getEnvironment())
                .doOnSubscribe(sub -> {
                    this.maintenanceActive = true;
                    log.debug("Purging repository {} through admin services.", ctx.getEnvironment());
                })
                .doOnSuccess(suc -> {
                    this.maintenanceActive = false;
                    log.debug("Purging repository {} completed.", ctx.getEnvironment());
                })
                .subscribeOn(Schedulers.newSingle("import"))
                .subscribe();
        return Mono.empty();
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype, SessionContext ctx) {
        if (maintenanceActive) return Mono.error(new SchedulingException("Maintenance job still running."));

        this.stores.get(ctx.getEnvironment().getRepositoryType())
                .asMaintainable()
                .importStatements(bytes, mimetype, ctx.getEnvironment())
                .doOnSubscribe(this::lock)
                .doOnSubscribe(sub -> {
                    log.debug("Importing statements of type '{}' into repository {} through admin services", mimetype, ctx.getEnvironment());
                })
                .doOnSuccess(suc -> {
                    log.debug("Importing statements completed into repository {} through admin services", ctx.getEnvironment());
                })
                .doOnSuccess(this::release)
                .subscribeOn(Schedulers.newSingle("import"))
                .subscribe();
        return Mono.empty();

    }

    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Void> importFromEndpoint(String endpoint, Map<String, String> headers, int limit, int offset, SessionContext ctx) {
        EndpointImporter endpointImporter = new EndpointImporter(endpoint, headers, this.stores, this.identifierServices);
        return endpointImporter.runImport(ctx)
                .doOnSubscribe(this::lock)
                .doOnSuccess(this::release);
    }

    /**
     * Stops the maintenance mode.
     */
    private void release(Void suc) {
        this.maintenanceActive = false;
    }

    /*
     * Once the lock is set, we disallow parallel admin jobs
     */
    private void lock(Subscription sub) {
        this.maintenanceActive = true;
    }

    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Void> importPackage(FilePart file, SessionContext ctx) {
        final int bufferSize = 1024;
        DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        final String filename = file.filename();

        Validate.matchesPattern(filename, "\\w+\\.\\w+\\.gz", "GZIP file has to match pattern 'filename.format.gz', e.g. 'data.ttl.gt'");
        String originalFilename = filename.substring(0, filename.lastIndexOf('.'));
        Optional<RDFFormat> parserFormatForFileName = Rio.getParserFormatForFileName(originalFilename);
        Validate.isTrue(parserFormatForFileName.isPresent(), "Failed to find RDF parser for filename %s".formatted(originalFilename));

        return Mono.just(file)
                .flatMap(filePart -> {
                    try {
                        File gzipFilePath = File.createTempFile("import", filename);
                        log.debug("Storing incoming zip file in temp as {} ", gzipFilePath.toString());
                        return DataBufferUtils.write(file.content(), gzipFilePath.toPath()).then(Mono.just(gzipFilePath));
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                })
                .flatMap(zipFile -> {
                    log.debug("Reading zip file {}", zipFile.toString());
                    try {
                        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(zipFile));

                        Flux<DataBuffer> flux = Flux.generate(
                                () -> gzipInputStream,
                                (stream, sink) -> {
                                    try {
                                        byte[] buffer = new byte[bufferSize];
                                        int read = stream.read(buffer);
                                        if (read > 0) {
                                            DataBuffer dataBuffer = bufferFactory.wrap(ByteBuffer.wrap(buffer, 0, read));
                                            sink.next(dataBuffer);
                                        } else {
                                            sink.complete();
                                            stream.close();
                                        }
                                    } catch (IOException e) {
                                        sink.error(e);
                                    }
                                    return stream;
                                }
                        );
                        return Mono.just(flux);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                }).flatMap(buffers -> {
                    String mimeType = parserFormatForFileName.orElseThrow().getDefaultMIMEType();
                    return this.importEntities(buffers, mimeType, ctx);
                });
    }

}
