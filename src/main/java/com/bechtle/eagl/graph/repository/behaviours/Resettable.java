package com.bechtle.eagl.graph.repository.behaviours;

import com.bechtle.eagl.graph.api.converter.RdfUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.Optional;

public interface Resettable extends RepositoryBehaviour {

    default Mono<Void> reset() {
        return Mono.create(m -> {
            try (RepositoryConnection connection = getRepository().getConnection()) {
                RepositoryResult<Statement> statements = connection.getStatements(null, null, null);
                connection.remove(statements);
                m.success();
            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    default Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype) {
        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file.");
        RDFParser parser = parserFactory.orElseThrow().getParser();


        Mono<DataBuffer> joined = DataBufferUtils.join(bytesPublisher);


        return joined.flatMap(bytes -> {
            try (RepositoryConnection connection = getRepository().getConnection()) {
                RDFInserter rdfInserter = new RDFInserter(connection);
                parser.setRDFHandler(rdfInserter);
                try (InputStream bais = bytes.asInputStream(true)) {
                    parser.parse(bais);
                } catch (Exception e) {
                    return Mono.error(e);
                }
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(e);
            }

        }).then();
    }
}
