package com.bechtle.eagl.graph.connector.rdf4j.repository;

import com.bechtle.eagl.graph.connector.rdf4j.model.ResourceIdentifier;
import com.bechtle.eagl.graph.model.Identifier;
import com.bechtle.eagl.graph.model.Triples;
import com.bechtle.eagl.graph.repository.Graph;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class TripleStore implements Graph {

    private final Repository repository;
    private final SecureRandom secureRandom;

    public TripleStore(Repository repository) {
        this.repository = repository;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public Flux<Identifier> store(Triples triples) {
        return Flux.create(c -> {
            // generate unique identifier
            Rio.write(triples.getStatements(), Rio.createWriter(RDFFormat.NQUADS, System.out));

            // perform validation via shacl
            // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html


            // get statements and load into repo
            try(RepositoryConnection connection = repository.getConnection()) {
                try {
                    connection.begin();
                    connection.add(triples.getStatements());
                    connection.commit();
                    c.next(new ResourceIdentifier(SimpleValueFactory.getInstance().createIRI("http://www.example.org", "hello")));
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            } finally {
                c.complete();
            }

        });
    }

    @Override
    public Mono<Triples> get(Identifier id) {
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
