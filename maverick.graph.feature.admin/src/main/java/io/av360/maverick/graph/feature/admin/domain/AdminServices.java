package io.av360.maverick.graph.feature.admin.domain;

import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.RepositoryType;
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
//import software.amazon.awssdk.core.ResponseBytes;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.GetObjectRequest;
//import software.amazon.awssdk.services.s3.model.GetObjectResponse;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.model.S3Exception;
import java.io.StringWriter;
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

    public Flux<Statement> exportEntities(Authentication authentication) {
        return this.graph.listStatements(null, null, null, authentication).map(statements -> {
            StringWriter stringWriter = new StringWriter();
            RDFWriter writer = RDFWriterRegistry.getInstance().get(RDFFormat.TURTLE).get().getWriter(stringWriter);
            writer.startRDF();
            statements.forEach(writer::handleStatement);
            writer.endRDF();

            return statements;
        })
                .flatMapMany(Flux::fromIterable)
                .doOnSubscribe(sub -> log.info("Exporting statements through admin services"));
    }

    public Mono<Void> exportToS3(Authentication authentication) {
        return exportEntities(authentication)
                .collectList()
                .flatMap(statements -> {
                    System.out.println(statements.size());
//                    try {
//                        S3Client s3 = S3Client.builder()
//                                .endpointOverride(URI.create(ba.asString(s3Host)))
//                                .build();
//                        PutObjectRequest objectRequest = PutObjectRequest.builder()
//                                .bucket(ba.asString(s3Bucket))
//                                .key(exportIdentifier)
//                                .build();
//                        s3.putObject(objectRequest, RequestBody.fromString(ba.asString(node)));
//
//                    } catch (S3Exception e) {
//                        log.error(e.awsErrorDetails().errorMessage());
//                    }
//
                    return Mono.empty();
                })
                .doOnSubscribe(sub -> log.info("Exporting statements to S3 through admin services"))
                .then();
    }
}
