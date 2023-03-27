package io.av360.maverick.graph.api.converter.encoder;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.*;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "graph.ctrl.io.encoder.bindings")
public class BindingSetEncoder implements Encoder<BindingSet> {
    private static final List<MimeType> mimeTypes;

    static {
        mimeTypes = List.of(
                MimeType.valueOf(TupleQueryResultFormat.JSON.getDefaultMIMEType()),
                MimeType.valueOf(TupleQueryResultFormat.CSV.getDefaultMIMEType())
        );
    }


    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null
                && BindingSet.class.isAssignableFrom(elementType.toClass())
                && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return mimeTypes;
    }


    @Override
    public Flux<DataBuffer> encode(Publisher<? extends BindingSet> publisher, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {


        Assert.notNull(mimeType, "No mimetype is set");
        Assert.isAssignable(BindingSet.class, elementType.toClass(), "Invalid object definition");


        // this cannot be outside of flux


        QueryResultFormat format = QueryResultIO.getParserFormatForMIMEType(mimeType.toString()).orElseThrow();
        TupleQueryResultWriterFactory tupleQueryResultWriterFactory = TupleQueryResultWriterRegistry.getInstance().get(format).orElseThrow();
        DefaultDataBuffer dataBuffer = new DefaultDataBufferFactory().allocateBuffer();
        /*
            FIXME: we don't really stream the flux, we have to rebuild the tuplequeryresultwriter

         */

        return Flux.from(publisher)
                .collectList()
                .flatMapMany(bindingSetList -> {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        if (!bindingSetList.isEmpty()) {
                            TupleQueryResultBuilder b = new TupleQueryResultBuilder();
                            b.startQueryResult(bindingSetList.get(0).getBindingNames().stream().toList());
                            bindingSetList.forEach(b::handleSolution);
                            b.endQueryResult();
                            QueryResultIO.writeTuple(b.getQueryResult(), format, baos);
                        }

                        return Flux.just(bufferFactory.wrap(baos.toByteArray()));


                    } catch (TupleQueryResultHandlerException | UnsupportedQueryResultFormatException e) {
                        log.warn("Failed to write query response of mimetype '{}'", mimeType, e);
                        return Flux.error(new ResponseStatusException(HttpStatus.CONFLICT, "Failed to handle query results"));
                    } catch (Exception e) {
                        log.error("Failed to write query response of mimetype '{}'", mimeType, e);
                        return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write response"));
                    }
                });


    }

}
