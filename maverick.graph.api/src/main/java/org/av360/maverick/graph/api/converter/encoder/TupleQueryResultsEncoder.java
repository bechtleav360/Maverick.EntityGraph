package org.av360.maverick.graph.api.converter.encoder;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.UnsupportedQueryResultFormatException;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "graph.ctrl.io.encoder.query")
public class TupleQueryResultsEncoder implements Encoder<TupleQueryResult> {
    private static final List<MimeType> mimeTypes;

    static {
        mimeTypes = List.of(
                MimeType.valueOf(TupleQueryResultFormat.JSON.getDefaultMIMEType()),
                MimeType.valueOf(TupleQueryResultFormat.CSV.getDefaultMIMEType())
        );
    }


    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return false;
        /*
        return mimeType != null
                && TupleQueryResult.class.isAssignableFrom(elementType.toClass())
                && mimeType.isPresentIn(mimeTypes);

         */
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {

        return List.of();
        // return mimeTypes;
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends TupleQueryResult> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {


        Assert.notNull(mimeType, "No mimetype is set");
        Assert.isAssignable(TupleQueryResult.class, elementType.toClass(), "Invalid object definition");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            QueryResultFormat format = QueryResultIO.getParserFormatForMIMEType(mimeType.toString()).orElseThrow();

            return Flux.from(inputStream)
                    .doOnSubscribe(subscription -> log.debug("Writing tuple query results response with mimetype '{}'", mimeType))
                    .doOnComplete(() -> {
                        log.trace("Completed writing tuple query results response with mimetype '{}'", mimeType);

                    })
                    .flatMap(bindings -> {
                        try {
                            QueryResultIO.writeTuple(bindings, format, baos);
                            return Flux.just(bufferFactory.wrap(baos.toByteArray()));
                        } catch (TupleQueryResultHandlerException | UnsupportedQueryResultFormatException e) {
                            log.warn("Failed to write query response of mimetype '{}'", mimeType, e);
                            return Flux.error(new ResponseStatusException(HttpStatus.CONFLICT, "Failed to handle query results"));
                        } catch (IOException e) {
                            log.error("Failed to write query response of mimetype '{}'", mimeType, e);
                            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response"));
                        }
                    });

        } catch (IOException e) {
            log.error("Failed to write into stream with mimetype '{}'", mimeType, e);
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response"));
        }
    }

}
