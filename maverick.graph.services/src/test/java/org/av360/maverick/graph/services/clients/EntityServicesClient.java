package org.av360.maverick.graph.services.clients;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.store.rdf.helpers.TriplesCollector;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EntityServicesClient {

    private final EntityServices entityServices;

    private final QueryServices queryServices;

    private final EntityStore entityStore;



    public EntityServicesClient(EntityServices entityServices, QueryServices queryServices, EntityStore entityStore) {
        this.entityServices = entityServices;
        this.queryServices = queryServices;
        this.entityStore = entityStore;
    }

    private RDFFormat getFormat(Resource resource) throws IOException {
        if(resource != null && resource.exists()) {
            if (resource.getFilename().endsWith("ttl")) return RDFFormat.TURTLE;
            if (resource.getFilename().endsWith("jsonld")) return  RDFFormat.JSONLD;
            if (resource.getFilename().endsWith("n3")) return  RDFFormat.N3;
            if (resource.getFilename().endsWith("xml")) return  RDFFormat.RDFXML;
        }
        throw new IOException("Resource does not exist: "+resource);
    }

    public Mono<Void> importFileToStore(Resource resource) throws IOException {
        Flux<DataBuffer> read = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 1024);
        SessionContext testContext = TestSecurityConfig.createTestContext();

        return this.entityStore
                .importStatements(read, getFormat(resource).getDefaultMIMEType(), testContext.getEnvironment())
                .doOnSubscribe(subscription -> log.info("Importing file '{}'", resource.getFilename()));
    }

    public void importFile(Resource resource) throws IOException {
        importFileMono(resource).block();
    }

    public Mono<RdfTransaction> importFileMono(Resource resource) throws IOException {
        Flux<DataBuffer> read = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 1024);
        return this.parse(read, getFormat(resource))
                .flatMap(triples -> this.entityServices.create(triples, Map.of(), TestSecurityConfig.createTestContext()))
                .doOnSubscribe(s -> log.info("Importing file '{}'", resource.getFilename()))
                .doOnError(s -> log.error("Failed to import file '{}'", resource.getFilename()));
    }

    public Mono<Model> getModel() {
        ModelBuilder builder = new ModelBuilder();

        return this.statements()
                .doOnNext(st -> builder.add(st.getSubject(), st.getPredicate(), st.getObject()))
                .then(Mono.just(builder.build()))
                .doOnSubscribe(sub -> log.trace("Reading model for assertions"));
    }

    public Flux<AnnotatedStatement> statements() {
        Variable s = SparqlBuilder.var("s");
        Variable p = SparqlBuilder.var("p");
        Variable o = SparqlBuilder.var("o");
        ConstructQuery q = Queries.CONSTRUCT().where(GraphPatterns.tp(s, p, o));

        return this.queryServices.queryGraph(q, TestSecurityConfig.createTestContext())
                .doOnSubscribe(sub -> log.info("Querying with: {}", q.getQueryString()));
    }

    public Mono<List<AnnotatedStatement>> listAllStatements() {
       return this.statements().collectList();
    }



    public List<RdfEntity> listAllEntities() {
        return listAllEntitiesMono().block();
    }

    public Mono<List<RdfEntity>> listAllEntitiesMono() {
        return this.entityServices.list(100, 0, TestSecurityConfig.createTestContext()).collectList();
    }

    private Mono<Triples> parse(Publisher<DataBuffer> publisher, RDFFormat format) {

        return DataBufferUtils.join(publisher)
                .flatMap(dataBuffer -> {
                    RDFParser parser = RDFParserRegistry.getInstance().get(format).orElseThrow().getParser();
                    TriplesCollector handler = RdfUtils.getTriplesCollector();

                    try (InputStream is = dataBuffer.asInputStream(true)) {
                        parser.setRDFHandler(handler);
                        parser.parse(is);
                        return Mono.just(handler.getTriples());
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .doOnSubscribe(s -> log.trace("Parsing file with format '{}'", format.getName()));
    }


}
