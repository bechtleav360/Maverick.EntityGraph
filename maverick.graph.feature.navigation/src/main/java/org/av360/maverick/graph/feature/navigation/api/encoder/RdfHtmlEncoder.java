package org.av360.maverick.graph.feature.navigation.api.encoder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Transforms a set of statements to navigable HTML
 */
@Slf4j(topic = "graph.ctrl.io.encoder.html")
public class RdfHtmlEncoder implements Encoder<Statement> {


    private final SimpleValueFactory vf;

    public RdfHtmlEncoder() {
        this.vf = SimpleValueFactory.getInstance();
    }

    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && mimeType.equals(MimeTypeUtils.TEXT_HTML);
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
                .filter(statement -> ! statement.getObject().equals(Local.Entities.INDIVIDUAL))
                .filter(statement -> ! statement.getObject().equals(Local.Entities.EMBEDDED))
                .filter(statement -> ! statement.getObject().equals(Local.Entities.CLASSIFIER))
                .filter(statement -> ! statement.getPredicate().equals(Local.ORIGINAL_IDENTIFIER))
                .collectList()
                .flatMap(list ->  Mono.zip(Mono.just(list), ReactiveRequestUriContextHolder.getURI()))
                .flatMapMany(tuple -> {
                    List<Statement> statements = tuple.getT1();
                    URI requestURI = tuple.getT2(); // we need the request to resolve the current request url

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        RDFWriter writer = getWriter(baos, requestURI);

                        writer.startRDF();

                        if(statements.size() > 0) {
                            this.handleNamespaces(writer, statements.get(0), requestURI);
                        }

                        for (Statement st : statements) {
                            this.handleStatement(st, writer, requestURI);

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

    private void handleStatement(Statement st, RDFWriter writer, URI requestURI) {

        Resource subject = convertLocalIRI(st.getSubject(), requestURI);
        IRI predicate = convertLocalIRI(st.getPredicate(), requestURI);
        Value object = convertLocalIRI(st.getObject(), requestURI);

        Statement convertedStatement = vf.createStatement(subject, predicate, object);
        writer.handleStatement(convertedStatement);
    }

    private <T extends Value> T convertLocalIRI(T value, URI requestURI) {

        if(value instanceof IRI iri) {
            if(iri.getNamespace().startsWith(Local.URN_PREFIX)) {

                /* ok, here the application module is leaking into the default implementation. If the IRI namespace includes a qualifier,
                   we inject it as scope. To make it reproducible, we assume: if namespace is not default namespace, we take the (URL decoded) string
                   attached and place it under scope ("s").

                   Examples:
                    urn:pwid:meg:e:,213 -> /api/entities/213
                    urn:pwid:meg:e:, f33.213 -> /api/entities/s/f33/213
                    urn:pwid:meg:, 123 -> /
                 */
                String[] parts = iri.getLocalName().split("\\.");
                String ns = iri.getNamespace();
                String path = "";

                if(ns.startsWith(Local.Entities.NAMESPACE)) {
                    if(parts.length == 1) {
                        path += "/api/entities/"+parts[0];
                    } else {
                        path += "/api/s/"+parts[0]+"/entities/"+parts[1];
                    }

                }

                else if(iri.getNamespace().startsWith(Local.Transactions.NAMESPACE)) {
                    path = "/api/transactions/"+parts[0];
                }

                else if(iri.getNamespace().startsWith(Local.Applications.NAMESPACE)) {
                    path = "/api/applications/"+parts[0];
                }

                else {
                    path = parts[0];
                }

                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }
        } else if(value instanceof Literal literal) {
            if(literal.stringValue().startsWith("?/")) {
                String p = literal.stringValue().substring(2);
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(p).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }
        }
        return value;

    }


    private RDFWriter getWriter(OutputStream out, URI requestURI) {
        MimeType turtle = MimeType.valueOf(RDFFormat.TURTLE.getDefaultMIMEType());
        TurtleWriter writer = (TurtleWriter) RdfUtils.getWriterFactory(turtle).get().getWriter(out);
        return new TurtleHtmlWriter(writer, out, requestURI);
    }



    private boolean handleNamespaces(RDFWriter writer, Statement statement, URI requestURI) {
        if (NamespaceAware.class.isAssignableFrom(statement.getClass())) {
            Set<Namespace> namespaces = ((NamespaceAware) statement).getNamespaces();

            namespaces.forEach(ns -> {
                // local URNs are ignored by default
                String url = ns.getName();
                if(url.startsWith("urn:pwid:eg:")) return;
                if(url.startsWith("?")) {
                    String p = url.substring(2);
                    url = UriComponentsBuilder.fromUri(requestURI).replacePath(p).replaceQuery("").build().toUriString();
                }

                writer.handleNamespace(ns.getPrefix(), url);
            });
            return true;
        }
        return false;
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return List.of(MimeTypeUtils.TEXT_HTML);
    }
}
