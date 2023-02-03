package io.av360.maverick.graph.api.converter.decoder;

import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import io.av360.maverick.graph.store.rdf.helpers.TriplesCollector;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "graph.api.decoder")
public class StatementsDecoder implements Decoder<TripleBag> {
    private static final List<MimeType> mimeTypes;

    static {
        mimeTypes = List.of(
                MimeType.valueOf(RDFFormat.JSONLD.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.RDFJSON.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.NTRIPLES.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.N3.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.TURTLE.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.NQUADS.getDefaultMIMEType())
        );
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return mimeTypes;
    }

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && TripleModel.class.isAssignableFrom(elementType.toClass()) && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public Flux<TripleBag> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(this.parse(inputStream, mimeType));
    }

    @Override
    public Mono<TripleBag> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return this.parse(inputStream, mimeType);
    }


    private Mono<TripleBag> parse(Publisher<DataBuffer> publisher, MimeType mimeType) {

        return DataBufferUtils.join(publisher)
                .flatMap(dataBuffer -> {

                    log.debug("Trying to parse payload of mimetype '{}'", mimeType.toString());
                    RDFParser parser = RdfUtils.getParserFactory(mimeType).orElseThrow().getParser();
                    TriplesCollector handler = RdfUtils.getTriplesCollector();

                    try (InputStream is = dataBuffer.asInputStream(true)) {
                        parser.setRDFHandler(handler);
                        parser.parse(is);
                        log.trace("Parsing of payload with mimetype '{}' completed", mimeType);
                        return Mono.just(handler.getModel());
                    } catch (Exception e) {
                        log.error("Failed to parse request of mimetype '{}'", mimeType, e);
                        return Mono.error(e);
                    }
                });


    }


}
