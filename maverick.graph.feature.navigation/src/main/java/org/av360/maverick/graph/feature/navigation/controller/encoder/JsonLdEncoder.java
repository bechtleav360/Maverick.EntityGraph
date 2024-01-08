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

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdEmbed;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import jakarta.json.*;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.feature.navigation.services.rio.RdfDatasetCollector;
import org.av360.maverick.graph.services.NavigationServices;
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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                .flatMap(statement -> Mono.zip(Mono.just(statement), ReactiveRequestUriContextHolder.getURI()))
                .map(tuple -> EncoderFilters.resolveURLs(tuple.getT1(), tuple.getT2()))
                .filter(EncoderFilters::filterInternalTypeStatements)
                .collect(RdfDatasetCollector.toDataset())
                .flatMap(dataset -> Mono.zip(Mono.just(dataset), ReactiveRequestUriContextHolder.getURI()))
                .flatMap(tuple -> {

                    JsonLdOptions options = new JsonLdOptions();
                    options.setCompactArrays(true);
                    options.setOmitGraph(true);
                    options.setRdfStar(true);
                    // options.setUseRdfType(true);
                    options.setEmbed(JsonLdEmbed.ALWAYS);

                    try {
                        Document document = RdfDocument.of(tuple.getT1());
                        JsonArray jsonValues = JsonLd.fromRdf(document).options(options).get();
                        JsonDocument jsonDocument = null;

                        if(jsonValues.size() == 1) {
                            jsonDocument = JsonDocument.of(jsonValues.get(0).asJsonObject());
                        } else {
                            jsonDocument = JsonDocument.of(jsonValues);
                        }


                        String uri = UriComponentsBuilder.fromUri(tuple.getT2()).replacePath(tuple.getT2().getPath()+"/context").toUriString();

                        JsonObject jsonObject = JsonLd.frame(jsonDocument, uri).options(options).get();
                        // FromRdfApi fromRdfApi = JsonLd.fromRdf(document);
                        // fromRdfApi.
                        // return Mono.just(JsonLd.fromRdf(document).options(options).base(tuple.getT2()).get());
                        return Mono.just(jsonObject);
                    } catch (JsonLdError e) {
                        return Mono.error(e);
                    }
                }).flatMapMany(json -> {
                    JsonWriterFactory writerFactory = JsonProvider.provider().createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));
                    Flux<DataBuffer> dataBufferFlux = Flux.empty();

                    try (StringWriter stringWriter = new StringWriter();
                         JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
                        jsonWriter.write(json); // Write JSON object to writer
                        String jsonString = stringWriter.toString();
                        byte[] elementBytes = jsonString.getBytes(StandardCharsets.UTF_8); // Convert the element to bytes
                        DataBuffer dataBuffer = bufferFactory.wrap(elementBytes); // Wrap bytes in DataBuffer
                        return dataBufferFlux.concatWith(Flux.just(dataBuffer));

                    } catch (IOException e) {
                        return Flux.error(e);
                    }
                });
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return List.of(MimeTypeUtils.TEXT_HTML);
    }


    private JsonObject createContext(Set<String> namespaces) {
        JsonObjectBuilder context = Json.createObjectBuilder();
        namespaces.forEach(namespace -> {
            this.schemaStore.getPrefixForNamespace(namespace).ifPresent(s -> context.add(s, namespace));
        });

        context.add("data", "@graph");
        context.add("id", "@id");
        context.add("type", "@type");
        context.add("value", "@value");
        context.add("annotations", "@annotations");

        JsonObjectBuilder memberDefinition = Json.createObjectBuilder()
                .add("@id", NavigationServices.DATA_CONTEXT.stringValue())
                .add("data", Json.createObjectBuilder().add("@type", "{}").build());


        return Json.createObjectBuilder()
                .add("@context", context.build())
                .add("@type", "hydra:Collection")
                .add("hydra:member", memberDefinition.build())
                .build();

    }
}
