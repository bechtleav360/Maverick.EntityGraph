package io.av360.maverick.graph.tests.util;

import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.model.security.SystemAuthentication;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.TransactionsStore;
import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

public abstract class TestsBase {

    protected static ValueFactory vf = SimpleValueFactory.getInstance();


    protected WebTestClient webClient;
    private EntityStore entityStore;
    private TransactionsStore transactionsStore;

    @Autowired
    public void setWebClient(WebTestClient webClient) {

        this.webClient = webClient;
    }

    @Autowired
    public void setStores(EntityStore entityStore, TransactionsStore transactionsStore) {

        this.entityStore = entityStore;
        this.transactionsStore = transactionsStore;
    }


    protected void dump() {

        CsvConsumer csvConsumer = new CsvConsumer();
        webClient
                .post()
                .uri("/api/query/select")
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .body(BodyInserters.fromValue("SELECT DISTINCT * WHERE { ?s ?p ?o }"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(csvConsumer);

        System.out.println(csvConsumer.getAsString());

    }


    protected void importEntities(Resource file) {
        RDFFormat format = null;
        if (file.getFilename().endsWith("ttl")) format = RDFFormat.TURTLE;
        if (file.getFilename().endsWith("jsonld")) format = RDFFormat.JSONLD;
        if (file.getFilename().endsWith("n3")) format = RDFFormat.N3;
        if (file.getFilename().endsWith("xml")) format = RDFFormat.RDFXML;

        Flux<DataBuffer> read = DataBufferUtils.read(file, new DefaultDataBufferFactory(), 1024);

        this.entityStore.importStatements(read, format.getDefaultMIMEType(), new SystemAuthentication(), Authorities.SYSTEM).block();
    }

    protected void resetRepository(String repositoryType) {
        this.entityStore.reset(new SystemAuthentication(), RepositoryType.ENTITIES, Authorities.SYSTEM);
        this.entityStore.reset(new SystemAuthentication(), RepositoryType.TRANSACTIONS, Authorities.SYSTEM);
        /*
        webClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/admin/bulk/reset")
                                .queryParam("name", repositoryType)
                                .build()

                )
                .exchange()
                .expectStatus().isAccepted();
                */
    }

    protected RdfConsumer upload(String path) {
        Resource file = new ClassPathResource(path);
        return this.upload(file);

    }

    protected RdfConsumer upload(Resource file) {

        RDFFormat format = null;
        if (file.getFilename().endsWith("ttl")) format = RDFFormat.TURTLE;
        if (file.getFilename().endsWith("jsonld")) format = RDFFormat.JSONLD;
        if (file.getFilename().endsWith("n3")) format = RDFFormat.N3;
        if (file.getFilename().endsWith("xml")) format = RDFFormat.RDFXML;

        RdfConsumer rdfConsumer = new RdfConsumer(format, true);

        webClient.post()
                .uri("/api/entities")
                .contentType(RdfUtils.getMediaType(format))
                .accept(RdfUtils.getMediaType(format))
                .body(BodyInserters.fromResource(file))
                .header("X-API-KEY", "test")
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(rdfConsumer);

        return rdfConsumer;
    }

    protected RdfConsumer loadEntity(IRI subject) {

        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.TURTLE, true);
        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}")
                        .build(
                                subject.getLocalName()
                        )

                )
                .accept(RdfUtils.getMediaType(RDFFormat.TURTLE))
                .header("X-API-KEY", "test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);
        return rdfConsumer;

    }
}
