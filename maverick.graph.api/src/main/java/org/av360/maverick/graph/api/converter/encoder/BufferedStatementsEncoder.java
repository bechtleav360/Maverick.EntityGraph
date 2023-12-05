package org.av360.maverick.graph.api.converter.encoder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The buffered statements encoder is required by formats with a header (JSON-LD, Turtle) and a concise syntax. Here,
 * we need to collect all statements to print a completed document. For n-quads or similar formats, we simply dump the statements.
 */
@SuppressWarnings("FieldCanBeLocal")
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
    private final Environment environment;


    public BufferedStatementsEncoder(@Autowired SchemaServices schemaServices, @Autowired Environment environment) {
        this.schemaServices = schemaServices;

        this.environment = environment;
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
                .filter(this::acceptStatement)
                .collectList()
                .flatMap(list -> Mono.zip(Mono.just(list), ReactiveRequestUriContextHolder.getURI()))
                .flatMapMany(tuple -> {

                    List<Statement> statements = tuple.getT1();
                    URI requestURI = tuple.getT2(); // we need the request to resolve the current request url

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        RDFWriter writer = getWriter(mimeType, baos);

                        writer.startRDF();

                        if (statements.size() > 0) {
                            this.handleNamespaces(writer, statements.get(0));
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

    private boolean acceptStatement(Statement statement) {
        if (this.environment.matchesProfiles("dev | persistent")) return true;

        if (statement.getObject().equals(Local.Entities.TYPE_INDIVIDUAL)) return false;
        if (statement.getObject().equals(Local.Entities.TYPE_CLASSIFIER)) return false;
        if (statement.getObject().equals(Local.Entities.TYPE_EMBEDDED)) return false;
        if (statement.getPredicate().equals(Local.ORIGINAL_IDENTIFIER)) return false;

        return true;
    }

    private void handleStatement(Statement st, RDFWriter writer, URI requestURI) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Value subject = normalizeIdentifiers(st.getSubject(), requestURI, vf);
        // IRI predicate = normalizeIdentifiers(st.getPredicate(), requestURI, vf);
        // Value object = normalizeIdentifiers(st.getObject(), requestURI, vf);




        // Statement convertedStatement = vf.createStatement(subject, predicate, object);
        // writer.handleStatement(convertedStatement);
        writer.handleStatement(st);
    }

    private Value normalizeIdentifiers(Resource value, URI requestURI, SimpleValueFactory vf) {
        if(value instanceof IRI iri) return normalizeIdentifiers(iri, requestURI, vf);
        else return value;
    }

    private Value normalizeIdentifiers(Triple value, URI requestURI, SimpleValueFactory vf) {
        return null;
    }

    private Value normalizeIdentifiers(IRI value, URI requestURI, SimpleValueFactory vf) {
        if (value.equals(Local.Entities.TYPE_CLASSIFIER) || value.equals(Local.Entities.TYPE_INDIVIDUAL) || value.equals(Local.Entities.TYPE_EMBEDDED)) {
            return value;
        }
        if (value.getNamespace().startsWith(Local.URN_PREFIX)) {
            return this.convertInternalURNtoURI(value, requestURI);
        }
        return value;
    }

    private Value normalizeIdentifiers(Literal value, URI requestURI, SimpleValueFactory vf) {
        return null;
    }

    private <T extends Value> T convertLocalIRI(T value, URI requestURI, SimpleValueFactory vf) {
        if (value instanceof IRI iri) {
            // we ignore type definitions
            if (iri.equals(Local.Entities.TYPE_CLASSIFIER) || iri.equals(Local.Entities.TYPE_INDIVIDUAL) || iri.equals(Local.Entities.TYPE_EMBEDDED)) {
                return (T) value;
            }
            if (iri.getNamespace().startsWith(Local.URN_PREFIX)) {
                return (T) this.convertInternalURNtoURI(iri, requestURI);
            }
        } else if (value instanceof Literal) {
            if (value.stringValue().startsWith("?/")) {
                String p = value.stringValue().substring(2);
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(p).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }
        } else if (value instanceof Triple triple) {
            Resource convertedResource = triple.getSubject();
            IRI convertedPredicate = triple.getPredicate();
            Value convertedObject = triple.getObject();

            if(triple.getSubject() instanceof IRI iri) {
               /* iri.getNamespace().startsWith(Local.URN_PREFIX) {

                } */
            }
            if (triple.getSubject().isIRI()) {
                if (((IRI) triple.getSubject()).getNamespace().startsWith(Local.URN_PREFIX)) {

                }

                this.convertInternalURNtoURI((IRI) triple.getSubject(), requestURI);
            }

            return (T) Values.triple(convertedResource, convertedPredicate, convertedObject);

        }
        return value;

    }


    private IRI convertInternalURNtoURI(IRI urn, URI requestURI) {
         /* ok, here the application module is leaking into the default implementation. If the IRI namespace includes a qualifier,
                   we inject it as scope. To make it reproducible, we assume: if namespace is not default namespace, we take the (URL decoded) string
                   attached and place it under scope ("s").

                   Examples:
                    urn:pwid:meg:e:,213 -> /api/entities/213
                    urn:pwid:meg:e:, f33.213 -> /api/entities/s/f33/213
                    urn:pwid:meg:, 123 -> /
                 */
        String[] parts = urn.getLocalName().split("\\.");
        String ns = urn.getNamespace();
        String path = "";

        if (ns.startsWith(Local.Entities.NAME)) {
            if (parts.length == 1) {
                path += "/api/entities/" + parts[0];
            } else {
                path += "/api/s/" + parts[0] + "/entities/" + parts[1];
            }

        } else if (urn.getNamespace().startsWith(Local.Transactions.NAME)) {
            path = "/api/transactions/" + parts[0];
        } else if (urn.getNamespace().startsWith(Local.Applications.NAME)) {
            path = "/api/applications/" + parts[0];
        } else {
            path = parts[0];
        }

        // Fallback (probably only for Mockrequests)

        try {
            if (requestURI.toString().startsWith("/")) {
                requestURI = new URI("http://example.com%s".formatted(requestURI));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path).replaceQuery("").build().toUriString();
        return Values.iri(uri);
    }

    private RDFWriter getWriter(MimeType mimeType, OutputStream out) {
        RDFWriter writer = factories.get(mimeType).getWriter(out);

        if (mimeType.equals(RdfMimeTypes.JSONLD)) {
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
                if (ns.getName().startsWith("urn:pwid:eg:")) return;

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
