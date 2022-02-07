package com.bechtle.eagl.graph.config.converter.decoder;

import com.bechtle.eagl.graph.config.converter.RdfUtils;
import com.bechtle.eagl.graph.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.model.wrapper.IncomingStatements;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
public class StatementsDecoder implements Decoder<IncomingStatements> {
    private static final List<MimeType> mimeTypes;

    static {
        mimeTypes = List.of(
                MimeType.valueOf(RDFFormat.JSONLD.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.RDFJSON.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.NTRIPLES.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.N3.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.NQUADS.getDefaultMIMEType())
        );
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return mimeTypes;
    }

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && AbstractModelWrapper.class.isAssignableFrom(elementType.toClass()) && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public Flux<IncomingStatements> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(this.parse(inputStream, mimeType));
    }

    @Override
    public Mono<IncomingStatements> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return this.parse(inputStream, mimeType);
    }




    private Mono<IncomingStatements> parse(Publisher<DataBuffer> publisher, MimeType mimeType) {

        return Mono.from(publisher).flatMap(dataBuffer -> {
                    RDFParser parser = RdfUtils.getParserFactory(mimeType).orElseThrow().getParser();
                    RdfUtils.TriplesCollector handler = RdfUtils.getTriplesCollector();

                    try (InputStream is = dataBuffer.asInputStream(true)) {
                        parser.setRDFHandler(handler);
                        parser.parse(is);
                        return Mono.just(handler.getModel());
                    } catch (RDFParseException e) {
                        log.error("Failed to parse request of mimetype '{}'", mimeType.toString(), e);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format"));
                    } catch (Exception e) {
                        return Mono.error(e);
                    } finally {
                        log.trace("(Decoder) Parsing of payload with mimetype '{}' completed", mimeType.toString());
                    }
        });


    }





}
