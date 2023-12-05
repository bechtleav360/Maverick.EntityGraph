package org.av360.maverick.graph.feature.navigation.controller.encoder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.NavigationServices;
import org.av360.maverick.graph.store.SchemaStore;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transforms a set of statements to navigable HTML
 */
@Slf4j(topic = "graph.ctrl.io.encoder.html")
public class RdfHtmlEncoder implements Encoder<Statement> {


    private final SimpleValueFactory vf;

    private final SchemaStore schemaStore;

    public RdfHtmlEncoder(SchemaStore schemaStore) {
        this.schemaStore = schemaStore;
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
                .collect(ModelCollector.toTreeModel())
                .map(this::convertCompositesToBlankNodes)
                .map(this::filterStatements)
                .map(this::extractNamespaces)
                .flatMap(model ->  Mono.zip(Mono.just(model), ReactiveRequestUriContextHolder.getURI()))
                .flatMapMany(tuple -> {
                    Model model = tuple.getT1();

                    URI requestURI = tuple.getT2(); // we need the request to resolve the current request url

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        RDFWriter writer = getWriter(baos, requestURI);

                        writer.startRDF();

                        model.getNamespaces().forEach(namespace -> writer.handleNamespace(namespace.getPrefix(), namespace.getName()));

                        for (Statement st : model) {
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


    private Model extractNamespaces(Model model) {
        model.forEach(statement -> {
            this.handleNamespace(statement, model);
        });
        return model;
    }

    private void handleNamespace(Statement s, Model m) {
        if(s.getSubject() instanceof IRI iri) {
            this.handleNamespaceIRI(iri, m);
        }
        if(s.getObject() instanceof IRI iri) {
            this.handleNamespaceIRI(iri, m);
        }
        this.handleNamespaceIRI(s.getPredicate(), m);
    }

    private void handleNamespaceIRI(IRI iri, Model m) {
        Optional<String> prefixForNamespace = this.schemaStore.getPrefixForNamespace(iri.getNamespace());
        prefixForNamespace.ifPresent(s -> m.setNamespace(s, iri.getNamespace()));
    }

    private Model filterStatements(Model model) {
        // we filter out any internal statements
        return model.stream()
                .filter(this::isLiteralWithCommonLanguageTag)
                .filter(statement -> ! statement.getObject().equals(Local.Entities.TYPE_INDIVIDUAL))
                .filter(statement -> ! statement.getObject().equals(Local.Entities.TYPE_CLASSIFIER))
                .filter(statement -> ! statement.getObject().equals(Local.Entities.TYPE_INDIVIDUAL))
                .filter(statement -> ! statement.getPredicate().equals(Local.ORIGINAL_IDENTIFIER))
                .collect(new ModelCollector());


    }

    private boolean isLiteralWithCommonLanguageTag(Statement statement) {
        if(statement.getObject() instanceof Literal literal) {
            if(literal.getLanguage().isPresent()) {
                return literal.getLanguage().map(lang -> lang.startsWith("en") || lang.startsWith("de") || lang.startsWith("fr") || lang.startsWith("es")).orElse(Boolean.FALSE);
            } else return true;
        }
        return true;
    }

    /** for navigational purposes, we convert the embedded objects (as long as there's only one fragment pointing to it) to an embedded object */
    private Model convertCompositesToBlankNodes(Model statements) {
        Set<Resource> subjects = statements.filter(null, null, Local.Entities.TYPE_EMBEDDED).subjects();
        // FIXME: check if multiple pointers to embedded (is inconsistent, but can happen)
        Map<Resource, BNode> mappings = subjects.stream().collect(Collectors.toMap(str -> str, str -> Values.bnode()));



        ModelBuilder modelBuilder = new ModelBuilder();
        statements.forEach(statement -> {
            if(subjects.contains(statement.getSubject())) {
                modelBuilder.add(mappings.get(statement.getSubject()), statement.getPredicate(), statement.getObject());
            } else if(statement.getObject().isResource() && subjects.contains((Resource) statement.getObject())) {
                modelBuilder.add(statement.getSubject(), statement.getPredicate(), mappings.get((Resource) statement.getObject()));
            } else {
                modelBuilder.add(statement.getSubject(), statement.getPredicate(), statement.getObject());
            }
        });
        return modelBuilder.build();


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
            if(iri.getNamespace().startsWith(NavigationServices.ResolvableUrlPrefix)) {
                String path = iri.stringValue().substring(NavigationServices.ResolvableUrlPrefix.length()+1);
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }

            if(iri.getNamespace().startsWith(Local.URN_PREFIX)) {

                /* ok, here the application module is leaking into the default implementation. If the IRI namespace includes a qualifier,
                   we inject it as scope. To make it reproducible, we assume: if namespace is not default namespace, we take the (URL decoded) string
                   attached and place it under scope ("s").

                   Examples:
                    urn:pwid:meg:e:,213 -> /api/entities/213
                    urn:pwid:meg:e:, f33.213 -> /api/entities/s/f33/213
                    urn:pwid:meg:, 123 ->  /api/
                 */
                String[] parts = iri.getLocalName().split("\\.");
                String ns = iri.getNamespace();
                String path = "";

                if(ns.startsWith(Local.Entities.NAME)) {
                    if(parts.length == 1) {
                        path += "/api/entities/"+parts[0];
                    } else {
                        path += "/api/s/"+parts[0]+"/entities/"+parts[1];
                    }

                }

                else if(iri.getNamespace().startsWith(Local.Transactions.NAME)) {
                    path = "/api/transactions/"+parts[0];
                }

                else if(iri.getNamespace().startsWith(Local.Applications.NAME)) {
                    path = "/api/applications/"+parts[0];
                }

                else {
                    path = parts[0];
                }

                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }
        } else if(value instanceof Literal literal) {
            if(literal.stringValue().startsWith(NavigationServices.ResolvableUrlPrefix)) {
                String path = literal.stringValue().substring(NavigationServices.ResolvableUrlPrefix.length()+1);
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }

            if(literal.stringValue().startsWith("?/")) {
                String path = literal.stringValue();
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path.substring(1)).replaceQuery("").build().toUriString();
                return (T) vf.createIRI(uri);
            }
        }
        return value;

    }


    private RDFWriter getWriter(OutputStream out, URI requestURI) throws IOException {
        MimeType mime = MimeType.valueOf(RDFFormat.TURTLESTAR.getDefaultMIMEType());
        RDFWriter writer = RdfUtils.getWriterFactory(mime).orElseThrow().getWriter(out);
        return new HtmlWriter(writer, out, requestURI);
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
