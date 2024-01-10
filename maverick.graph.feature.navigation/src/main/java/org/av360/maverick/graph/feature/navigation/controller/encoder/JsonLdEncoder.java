/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.feature.navigation.controller.encoder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.services.util.io.JsonLdWriter;
import org.av360.maverick.graph.services.util.io.RdfDatasetCollector;
import org.av360.maverick.graph.store.SchemaStore;
import org.eclipse.rdf4j.model.Statement;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "graph.ctrl.io.encoder.html")
public class JsonLdEncoder implements Encoder<Statement> {
    private final SchemaStore schemaStore;

    public JsonLdEncoder(SchemaStore schemaStore) {
        this.schemaStore = schemaStore;
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
                .filter(EncoderFilters::filterInternalTypeStatements)
                .flatMap(statement -> Mono.zip(Mono.just(statement), ReactiveRequestUriContextHolder.getURI()))
                .map(EncoderFilters::resolveURLs)
                .collect(RdfDatasetCollector.toDataset())
                .flatMap(dataset -> Mono.zip(
                        Mono.just(dataset),
                        ReactiveRequestUriContextHolder.getURI().map(uri -> UriComponentsBuilder.fromUri(uri).replacePath(uri.getPath()+"/context").build(Map.of()))
                ))
                .flatMap(JsonLdWriter::buildJsonObject)
                .flatMap(JsonLdWriter::serializeJsonObject)
                .map(this::wrapInHtml)
                .flatMapMany(serialized -> {
                    Flux<DataBuffer> dataBufferFlux = Flux.empty();
                    byte[] elementBytes = serialized.getBytes(StandardCharsets.UTF_8); // Convert the element to bytes
                    DataBuffer dataBuffer = bufferFactory.wrap(elementBytes); // Wrap bytes in DataBuffer
                    return dataBufferFlux.concatWith(Flux.just(dataBuffer));
                });
    }

    private String wrapInHtml(String jsonString) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <title>Maverick Entity Graph Navigation</title>
                    <meta charset="UTF-8">
                    <script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
                    <script src="/jquery.json-viewer.js"></script>
                    <link href="/jquery.json-viewer.css" type="text/css" rel="stylesheet">
                    <link rel="stylesheet" href="/style.css"></link>
                </head>
                <body>
                <div class="box">
                <div class="fragment">
                <pre id="json-renderer"></pre>
                </div>
                <div id="json">%s</div>
                </div>
                
                <script type="application/javascript">
                    $(document).ready(function() {
                        var jsonString = $('#json').text();
                        var data = JSON.parse(jsonString);
                        $('#json').hide();
                        $('#json-renderer').jsonViewer(data, {collapsed: false, withQuotes: true, withLinks: true});
                    });
                    
                </script>
                </body>
                </html>
                
                """.formatted(jsonString);
    }


    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return List.of(MimeTypeUtils.TEXT_HTML);
    }


}
