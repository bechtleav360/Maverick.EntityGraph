package com.bechtle.eagl.graph.config.converter;

import com.bechtle.eagl.graph.connector.rdf4j.model.MutableValueFactory;
import com.bechtle.eagl.graph.model.Triples;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.util.ErrorHandler;
import org.springframework.util.MimeType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RdfDecoder implements Decoder<Triples> {


    private final MutableValueFactory mutableValueFactory;

    public RdfDecoder() {

        this.mutableValueFactory = new MutableValueFactory();
    }

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        if(mimeType == null) return false;

        boolean present = this.getParserFactory(mimeType).isPresent();
        return present;
    }

    @Override
    public Flux<Triples> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return this.parse(inputStream, mimeType);
    }

    @Override
    public Mono<Triples> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return this.parse(inputStream, mimeType)
                .collect(Triples::new, (all,single) -> {
                    all.getStatements().addAll(single.getStatements());
                } );
    }


    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return getAvailableParserFactories().parallelStream()
                .map(rdfParserFactory -> rdfParserFactory.getRDFFormat().getMIMETypes())
                .mapMulti((mimetypes, consumer) ->  { for(String mimetype : mimetypes) consumer.accept(mimetype); })
                .map(mimetypeObj -> MimeType.valueOf(mimetypeObj.toString()))
                .collect(Collectors.toList());
    }

    private Flux<Triples> parse(Publisher<DataBuffer> publisher, MimeType mimeType) {

        return Flux.from(publisher).flatMap(dataBuffer -> {
                    RDFParser parser = getParserFactory(mimeType).orElseThrow().getParser();
                    ContextStatementCollector collector = new ContextStatementCollector(mutableValueFactory);
                    try (InputStream is = dataBuffer.asInputStream(true)) {
                        parser.setRDFHandler(collector);
                        parser.parse(is);
                        return Flux.just(new Triples(collector.getStatements()));
                    } catch (RDFParseException e) {
                        log.error("Failed to parse request of mimetype '{}'", mimeType.toString(), e);
                        return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format"));
                    } catch (IOException e) {
                        return Flux.error(e);
                    } catch (Exception e) {
                        return Flux.error(e);
                    }
                    finally {
                        log.trace("(Decoder) Parsing of payload with mimetype '{}' completed", mimeType.toString());
                    }
        });


    }


    private Optional<RDFParserFactory> getParserFactory(MimeType mimeType) {
        assert mimeType != null;
        Optional<RDFParserFactory> fact = getAvailableParserFactories()
                .parallelStream()
                .filter(rdfParserFactory -> rdfParserFactory.getRDFFormat().hasMIMEType(mimeType.toString()))
                .findFirst();
        if(!fact.isPresent()) {
            log.warn("No parser support for MimeType {}", mimeType.toString());
        }

        return fact;

    }

    private Collection<RDFParserFactory> getAvailableParserFactories() {
        return RDFParserRegistry.getInstance().getAll();
    }


}
