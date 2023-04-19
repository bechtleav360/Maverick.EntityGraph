package org.av360.maverick.graph.feature.admin.domain;

import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.RepositoryType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
//import software.amazon.awssdk.core.ResponseBytes;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.GetObjectRequest;
//import software.amazon.awssdk.services.s3.model.GetObjectResponse;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.model.S3Exception;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;


@Service
@Slf4j(topic = "graph.feat.admin.svc")
public class AdminServices {

    private final EntityStore graph;

    public AdminServices(EntityStore graph) {
        this.graph = graph;
    }


    public Mono<Void> reset(Authentication authentication, RepositoryType repositoryType) {
        return this.graph.reset(authentication, repositoryType, Authorities.SYSTEM)
                .doOnSubscribe(sub -> log.info("Purging repository through admin services"));
    }

    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype, Authentication authentication) {
        return this.graph.importStatements(bytes, mimetype, authentication, Authorities.APPLICATION)
                .doOnSubscribe(sub -> log.info("Importing statements of type '{}' through admin services", mimetype));
    }

    public Mono<PutObjectResponse> exportEntitiesToS3(Authentication authentication) {
        S3AsyncClient s3Client = S3AsyncClient.builder()
                .endpointOverride(URI.create("http://127.0.0.1:9000"))
                .region(Region.US_EAST_1)
                .build();

        return this.graph.listStatements(null, null, null, authentication)
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
                            .bucket("test")
                            .key("test")
                            .build();
                    return Mono.fromCompletionStage(s3Client.putObject(
                            putObjectRequest,
                            AsyncRequestBody.fromBytes(rdfString.getBytes(StandardCharsets.UTF_8))
                    ));
                })
                .doOnSubscribe(sub -> log.info("Exporting statements through admin services"));
    }



}
