package io.av360.maverick.graph.api.converter.encoder;

import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The buffered statements encoder is required by formats with a header (JSON-LD, Turtle) and a concise syntax. Here,
 * we need to collect all statements to print a completed document. For n-quads or similar formats, we simply dump the statements.
 */
@Slf4j(topic = "graph.api.encoder")
public class StatementsEncoder implements Encoder<NamespaceAwareStatement> {
    private static final List<MimeType> mimeTypes;


    static {
        mimeTypes = List.of(
                MimeType.valueOf(RDFFormat.RDFJSON.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.NTRIPLES.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.N3.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.NQUADS.getDefaultMIMEType())
        );
    }


    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && Statement.class.isAssignableFrom(elementType.toClass()) && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends NamespaceAwareStatement> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {


        return Flux
                .from(inputStream)
                .doOnSubscribe(c -> log.debug("Trying to write statements stream response with mimetype '{}'", mimeType != null ? mimeType.toString() : "unset"))
                .map(namespaceAwareStatement -> (Statement) namespaceAwareStatement)
                .buffer(5)
                .flatMap(statements -> {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        RDFWriter writer = RdfUtils.getWriterFactory(mimeType).orElseThrow().getWriter(baos);
                        Rio.write(statements, writer);
                        return Flux.just(bufferFactory.wrap(baos.toByteArray()));
                    } catch (IOException e) {
                        log.error("Failed to write response of mimetype '{}'", mimeType.toString(), e);
                        return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response"));
                    } finally {
                        log.trace("Completed writing statements stream response with mimetype '{}'", mimeType != null ? mimeType.toString() : "unset");
                    }

                });
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return mimeTypes;
    }
}

