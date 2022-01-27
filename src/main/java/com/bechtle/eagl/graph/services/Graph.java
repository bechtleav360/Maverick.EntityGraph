package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.Triples;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class Graph {

    private final Repository repository;

    public Graph(Repository repository) {
        this.repository = repository;
    }


    public Mono<Void> create(Triples of) {
        return Mono.create(c -> {
            // perform validation via shacl
            // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html

            // get statements and load into repo
            try(RepositoryConnection connection = repository.getConnection()) {
                try {
                    connection.begin();
                    connection.add(of.getStatements());
                    connection.commit();
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                }
            }
        });

    }
}
