package com.bechtle.eagl.graph.config.converter.encoder;

import com.bechtle.eagl.graph.config.converter.RdfUtils;
import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.NamespaceAware;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
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
import java.util.Optional;
import java.util.Set;

/**
 * The buffered statements encoder is required by formats with a header (JSON-LD, Turtle) and a concise syntax. Here,
 * we need to collect all statements to print a completed document. For n-quads or similar formats, we simply dump the statements.
 */
@Slf4j
public class BufferedStatementsEncoder implements Encoder<Statement> {
    private static final List<MimeType> mimeTypes;

    static {
        mimeTypes = List.of(
                MimeType.valueOf(RDFFormat.TURTLE.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.JSONLD.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.TURTLESTAR.getDefaultMIMEType())
        );
    }


    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && Statement.class.isAssignableFrom(elementType.toClass()) && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends Statement> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(inputStream)
                .map(statement -> (Statement) statement)
                .collectList()
                .flatMapMany(statements -> {
                    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        /* write namespaces taken from first statement in collection */
                        RDFWriter writer = RdfUtils.getWriterFactory(mimeType).orElseThrow().getWriter(baos);
                        statements.stream().findFirst().ifPresent(sts -> {
                                if(sts.getClass().isAssignableFrom(NamespaceAware.class)) {
                                    Set<Namespace> namespaces = ((NamespaceAware) sts).getNamespaces();
                                    namespaces.forEach(ns -> {
                                        writer.handleNamespace(ns.getPrefix(), ns.getName());
                                    });
                                }
                            });
                        Rio.write(statements, writer);

                        return Flux.just(bufferFactory.wrap(baos.toByteArray()));
                    } catch (IOException e) {
                        log.error("Failed to write response of mimetype '{}'", mimeType.toString(), e);
                        return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response"));
                    }
                });
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return mimeTypes;
    }
}
