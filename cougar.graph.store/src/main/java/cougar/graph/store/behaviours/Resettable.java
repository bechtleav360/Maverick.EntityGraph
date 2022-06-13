package cougar.graph.store.behaviours;

import cougar.graph.store.RepositoryType;
import cougar.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.Optional;

public interface Resettable extends RepositoryBehaviour {

    Mono<Void> reset(Authentication authentication, RepositoryType repositoryType);



    // TODO: Move to implementation
    default Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Authentication authentication) {
        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file.");
        RDFParser parser = parserFactory.orElseThrow().getParser();

        return DataBufferUtils.join(bytesPublisher)
                .map(dataBuffer -> {
                    try (RepositoryConnection connection = getConnection(authentication)) {
                        RDFInserter rdfInserter = new RDFInserter(connection);
                        parser.setRDFHandler(rdfInserter);
                        try (InputStream bais = dataBuffer.asInputStream(true)) {
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
