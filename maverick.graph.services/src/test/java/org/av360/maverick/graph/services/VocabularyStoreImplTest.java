package org.av360.maverick.graph.services;


import org.eclipse.rdf4j.model.Namespace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
class VocabularyStoreImplTest {

    @Autowired
    SchemaServices schemaServices;

    @Test
    void listNamespaces() {
        Mono<Namespace> esco = schemaServices.getNamespaceFor("esco");

        StepVerifier.create(esco).assertNext(namespace -> namespace.getName().equalsIgnoreCase("http://data.europa.eu/esco/model#")).verifyComplete();

    }
}