package org.av360.maverick.graph.api.converter.encoder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.config.ReactiveRequestContextHolder;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MimeType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The buffered statements encoder is required by formats with a header (JSON-LD, Turtle) and a concise syntax. Here,
 * we need to collect all statements to print a completed document. For n-quads or similar formats, we simply dump the statements.
 */
@Slf4j(topic = "graph.ctrl.io.encoder.buffered")
public class BufferedStatementsEncoder implements Encoder<Statement> {
    private static final List<MimeType> mimeTypes;

    private static final Map<MimeType, RDFWriterFactory> factories;

    static {
        MimeType turtle = MimeType.valueOf(RDFFormat.TURTLE.getDefaultMIMEType());
        MimeType jsonld = MimeType.valueOf(RDFFormat.JSONLD.getDefaultMIMEType());
        MimeType turtlestar = MimeType.valueOf(RDFFormat.TURTLESTAR.getDefaultMIMEType());


        mimeTypes = List.of(turtle, jsonld, turtlestar);

        factories = Map.of(
                turtle, RdfUtils.getWriterFactory(turtle).orElseThrow(),
                jsonld, RdfUtils.getWriterFactory(jsonld).orElseThrow(),
                turtlestar, RdfUtils.getWriterFactory(turtlestar).orElseThrow()
        );

    }

    private final SchemaServices schemaServices;


    public BufferedStatementsEncoder(@Autowired SchemaServices schemaServices) {
        this.schemaServices = schemaServices;

    }

    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && Statement.class.isAssignableFrom(elementType.toClass()) && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends Statement> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {

        return Flux.from(inputStream)
                .doOnSubscribe(c -> {
                    if (log.isTraceEnabled()) {
                        log.trace("Setting up buffered statements stream for response with mimetype '{}'", mimeType != null ? mimeType.toString() : "unset");
                    }
                })
                .map(statement -> (Statement) statement)
                // we filter out any internal statements
                .filter(statement -> !statement.getObject().equals(Local.Entities.INDIVIDUAL))
                .collectList()
                .flatMap(list ->  Mono.zip(Mono.just(list), ReactiveRequestContextHolder.getRequest()))
                .flatMapMany(tuple -> {
                    List<Statement> statements = tuple.getT1();
                    ServerHttpRequest request = tuple.getT2(); // we need the request to resolve the current request url

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        RDFWriter writer = getWriter(mimeType, baos);

                        writer.startRDF();

                        if(statements.size() > 0) {
                            this.handleNamespaces(writer, statements.get(0));
                        }

                        for (Statement st : statements) {
                            this.handleStatement(st, writer, request);

                        }
                        writer.endRDF();

                        return Flux.just(bufferFactory.wrap(baos.toByteArray()));
                    } catch (IOException e) {
                        log.error("Failed to write response of mimetype '{}'", mimeType.toString(), e);
                        return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response"));
                    } finally {
                        if (log.isTraceEnabled()) {
                            log.trace("Completed writing buffered statements response with mimetype '{}'", mimeType != null ? mimeType.toString() : "unset");
                        }
                    }
                });
    }

    private void handleStatement(Statement st, RDFWriter writer, ServerHttpRequest request) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        Resource subject = convertLocalIRI(st.getSubject(), request, vf);
        IRI predicate = convertLocalIRI(st.getPredicate(), request, vf);
        Value object = convertLocalIRI(st.getObject(), request, vf);

        Statement convertedStatement = vf.createStatement(subject, predicate, object);
        writer.handleStatement(convertedStatement);
    }

    private <T extends Value> T convertLocalIRI(T value, ServerHttpRequest request, SimpleValueFactory vf) {
        if(value instanceof IRI iri) {

            if(iri.getNamespace().startsWith(Local.URN_PREFIX)) {

                /* ok, here the application module is leaking into the default implementation. If the IRI namespace includes a qualifier,
                   we inject it as scope
                 */
                String path = iri.getLocalName();

                if(iri.getNamespace().startsWith(Local.Entities.NAMESPACE)) {


                    path = "/api/entities/"+path;
                }

                if(iri.getNamespace().startsWith(Local.Transactions.NAMESPACE)) {
                    path = "/api/transactions/"+path;
                }

                if(iri.getNamespace().startsWith(Local.Subscriptions.NAMESPACE)) {
                    path = "/api/applications/"+path;
                }

                String uri = UriComponentsBuilder.fromUri(request.getURI()).replacePath(path).build().toUriString();
                return (T) vf.createIRI(uri);
            }
        }
        return value;

    }


    private RDFWriter getWriter(MimeType mimeType, OutputStream out) {
        RDFWriter writer = factories.get(mimeType).getWriter(out);

        if(mimeType.equals(RdfMimeTypes.JSONLD)) {
            writer.set(JSONLDSettings.HIERARCHICAL_VIEW, true);
            writer.set(JSONLDSettings.COMPACT_ARRAYS, true);
            writer.set(JSONLDSettings.OPTIMIZE, true);
            writer.set(JSONLDSettings.USE_NATIVE_TYPES, true);
            writer.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
        }

        return writer;

    }



    private boolean handleNamespaces(RDFWriter writer, Statement statement) {
        if (NamespaceAware.class.isAssignableFrom(statement.getClass())) {
            Set<Namespace> namespaces = ((NamespaceAware) statement).getNamespaces();

            namespaces.forEach(ns -> {
                // local URNs are ignored by default
                if(ns.getName().startsWith("urn:pwid:eg:")) return;

                writer.handleNamespace(ns.getPrefix(), ns.getName());
            });
            return true;
        }
        return false;
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return mimeTypes;
    }
}
