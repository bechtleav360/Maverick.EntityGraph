package com.bechtle.eagl.graph.services;

import com.apicatalog.rdf.Rdf;
import com.bechtle.eagl.graph.model.Identifier;
import com.bechtle.eagl.graph.model.Triples;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class Graph {

    private final Repository repository;
    private final SecureRandom secureRandom;

    public Graph(Repository repository) {
        this.secureRandom = new SecureRandom();
        this.repository = repository;
    }


    public Mono<Identifier> create(Triples of) {
        return Mono.create(c -> {
            // generate unique identifier
            Rio.write(of.getStatements(), Rio.createWriter(RDFFormat.NQUADS, System.out));

            // perform validation via shacl
            // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html


            // get statements and load into repo
            try(RepositoryConnection connection = repository.getConnection()) {
                try {
                    connection.begin();
                    connection.add(of.getStatements());
                    connection.commit();
                    c.success();
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            }
        });

    }

    public Mono<Triples> get(String id) {
        return Mono.create(c -> {
            try(RepositoryConnection connection = repository.getConnection()) {
                try {
                    c.success(new Triples());
                    // construct IRI from id (in which context)
                    // connection.getStatements()
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                }
            }
        });

    }


    private String generateRandomBase64Token() {
        byte[] token = new byte[16];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
    }
}
