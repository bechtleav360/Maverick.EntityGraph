package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.api.converter.RdfUtils;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.repository.Graph;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Flow;


@Service
public class AdminServices {

    private final Graph graph;

    public AdminServices(Graph graph) {
        this.graph = graph;
    }


    public Mono<Void> reset() {
        return this.graph.reset();
    }

    public Mono<Void> importEntities(Publisher<DataBuffer> bytes, String mimetype) {
        return this.graph.importStatements(bytes, mimetype);



    }
}
