package org.av360.maverick.graph.api.converter.decoder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.store.rdf.helpers.TriplesCollector;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Used for fileformats such as turtle or json-ld with headers (prefix information etc)
 *
 */
@Slf4j(topic = "graph.ctrl.io.decoder")
public class BufferedStatementsDecoder implements Decoder<Triples> {
    private static final List<MimeType> mimeTypes;

    static {
        mimeTypes = List.of(
                MimeType.valueOf(RDFFormat.JSONLD.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.RDFJSON.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.TURTLE.getDefaultMIMEType()),
                MimeType.valueOf(RDFFormat.TURTLESTAR.getDefaultMIMEType())
        );
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return mimeTypes;
    }

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        return mimeType != null && Triples.class.isAssignableFrom(elementType.toClass()) && mimeType.isPresentIn(mimeTypes);
    }

    @Override
    public Flux<Triples> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(this.parse(inputStream, mimeType));
    }

    @Override
    public Mono<Triples> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return this.parse(inputStream, mimeType);
    }


    private Mono<Triples> parse(Publisher<DataBuffer> publisher, MimeType mimeType) {
        RDFParser parser = RdfUtils.getParserFactory(mimeType).orElseThrow().getParser();
        return this.streamToTemporaryFile(publisher, parser.getRDFFormat().getDefaultFileExtension())
                .flatMap(path -> {
                    TriplesCollector handler = RdfUtils.getTriplesCollector();
                    parser.setRDFHandler(handler);

                    try(InputStream io = Files.newInputStream(path)) {
                        parser.parse(io);
                        log.debug("Parsed payload of mimetype '{}' with {} statements", mimeType.toString(), handler.getTriples().getModel().size());

                    } catch (IOException e) {
                        log.warn("Failed to parse request of mimetype '{}'", mimeType);
                        return Mono.error(e);
                    }
                    return Mono.just(handler.getTriples());
                });

    }


    private Mono<Path> streamToTemporaryFile(Publisher<DataBuffer> buffers, String extension) {
        try {
            Path tempFile = Files.createTempFile("upload", "."+extension);

            AsynchronousFileChannel channel = AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

            return DataBufferUtils.write(buffers, channel)
                    .doOnTerminate(() -> {
                        try {
                            channel.close();
                        } catch (IOException e) {
                            throw new RuntimeException("Error closing the file channel", e);
                        }
                    })
                    .doOnComplete(() ->  {
                        tempFile.toFile().deleteOnExit();
                    })
                    .then(Mono.just(tempFile));

        } catch (IOException e) {
            log.warn("Failed to write temporary file while uploading");
            return Mono.error(e);
        }

    }

}
