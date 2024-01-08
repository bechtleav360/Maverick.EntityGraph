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
import com.apicatalog.jsonld.json.JsonProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.json.*;
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
import java.util.stream.Collectors;

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

                        ArrayNode jacksonNode = JsonJacksonConverter.deepCopyJsonArray(jsonValues);


                        // add annotations by hand
                        jsonValues = this.expandWithAnnotations(jsonValues, tuple.getT1()).build();

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
                }).flatMap(json -> {
                    JsonWriterFactory writerFactory = JsonProvider.instance().createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));
                    try (StringWriter stringWriter = new StringWriter();  JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
                        jsonWriter.write(json); // Write JSON object to writer
                        return Mono.just(stringWriter.toString());
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                })
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

    private JsonArrayBuilder expandWithAnnotations(JsonArray valueArray, ExtendedRdfDataset dataset) {
        JsonArrayBuilder result = JsonProvider.instance().createArrayBuilder();
        valueArray.forEach(item -> {

            if(item.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObjectBuilder objectBuilder =  this.expandObjectWithAnnotations(item.asJsonObject(), dataset);

                result.add(objectBuilder);
            }
            else if(item.getValueType() == JsonValue.ValueType.ARRAY) {
                JsonArrayBuilder arrayBuilder = this.expandWithAnnotations(item.asJsonArray(), dataset);

                result.add(arrayBuilder);
            } else {

                result.add(item);
            }
        });
        return result;
    }

    private JsonObjectBuilder expandObjectWithAnnotations(JsonObject jsonObject, ExtendedRdfDataset dataset) {
        JsonObjectBuilder result = JsonProvider.instance().createObjectBuilder();
        jsonObject.forEach((k,v) -> {
            if(v.getValueType() == JsonValue.ValueType.OBJECT) {
                result.add(k, this.expandObjectWithAnnotations(v.asJsonObject(), k, jsonObject, dataset));
            } else if(v.getValueType() == JsonValue.ValueType.ARRAY) {
                result.add(k, this.expandArrayWithAnnotations(v.asJsonArray(), k, jsonObject, dataset));
            } else {
                result.add(k,v);
            }
        });
        return result;
    }

    private JsonArrayBuilder expandArrayWithAnnotations(JsonArray jsonArray, String parentField, JsonValue parentObject, ExtendedRdfDataset dataset) {
        JsonArrayBuilder result = JsonProvider.instance().createArrayBuilder();
        jsonArray.forEach(item -> {
            if(item.getValueType() == JsonValue.ValueType.ARRAY) {
                result.add(this.expandArrayWithAnnotations(item.asJsonArray(), null, jsonArray, dataset));
            } else if(item.getValueType() == JsonValue.ValueType.OBJECT) {
                result.add(this.expandObjectWithAnnotations(item.asJsonObject(), null, jsonArray, dataset));
            } else {
                result.add(item);
            }
        });

        return result;
    }

    private JsonObjectBuilder expandObjectWithAnnotations(JsonObject jsonObject, String parentField, JsonValue parentObject, ExtendedRdfDataset dataset) {
        if(! jsonObject.containsKey("@id")) {
            return JsonProvider.instance().createObjectBuilder(jsonObject);
        }

        JsonObjectBuilder result = JsonProvider.instance().createObjectBuilder();
        jsonObject.forEach((k,v) -> {
            if(k.equalsIgnoreCase("@id")) return;

            String subject = ((JsonString) jsonObject.get("@id")).getString();

            Set<ExtendedRdfDataset.Annotation> annotations = dataset.getAnnotationsByIdentifierAndProperty(subject, k);
            if(! annotations.isEmpty()) {
                if(v.getValueType() == JsonValue.ValueType.ARRAY) {
                    JsonArrayBuilder arrayCopy = JsonProvider.instance().createArrayBuilder();
                    // we have to compare the values to identify which particular value was annotated
                    v.asJsonArray().forEach(arrayItem -> {
                        if(arrayItem.getValueType() != JsonValue.ValueType.OBJECT || ! arrayItem.asJsonObject().containsKey("@value")) {
                            arrayCopy.add(arrayItem);
                        } else {
                            Set<ExtendedRdfDataset.Annotation> filteredAnnotations = annotations.stream()
                                    .filter(annotation -> arrayItem.asJsonObject().get("@value").getValueType() == JsonValue.ValueType.STRING && annotation.object().equalsIgnoreCase(((JsonString) arrayItem.asJsonObject().get("@value")).getString()))
                                    .collect(Collectors.toSet());
                            if(! filteredAnnotations.isEmpty()) {
                                JsonObjectBuilder jsonObjectBuilder = this.injectAnnotation(arrayItem, filteredAnnotations);
                                arrayCopy.add(jsonObjectBuilder);
                            } else {
                                arrayCopy.add(arrayItem);
                            }
                        }
                    });
                    result.add(k, arrayCopy);
                } else if(v.getValueType() == JsonValue.ValueType.OBJECT) {
                    result.add(k, injectAnnotation(v, annotations));
                } else {
                    // we annotations on direct values, we would need to convert it into an object. But at this moment,
                    // (no compacting happened yet), every property should be {@value}
                    result.add(k, v);
                }
            } else {
                result.add(k,v);
            }

        });
        return result;
    }

    private  JsonObjectBuilder injectAnnotation(JsonValue v, Set<ExtendedRdfDataset.Annotation> annotations) {
        JsonObjectBuilder annotationsObject = JsonProvider.instance().createObjectBuilder();
        annotations.forEach(annot -> {
            annotationsObject.add(annot.annotationPredicate(), annot.annotationValue());
        });
        JsonObjectBuilder currentObject = JsonProvider.instance().createObjectBuilder(v.asJsonObject());
        currentObject.add("@annotation", annotationsObject);

        return currentObject;
    }


    /*

    private ObjectNode xexpassndObjectWithAnnotations(ObjectNode value, String fieldName, ObjectNode parent, ExtendedRdfDataset dataset) {
        ObjectNode result = value.deepCopy();

        if(fieldName.equalsIgnoreCase("@id")) return result;

        String parentIdentifier = parent.get("@id").toString();
        // for some reason the identifier is double quoted, we have to remove them
        if(parentIdentifier.startsWith("\"") && parentIdentifier.endsWith(("\""))) {
            parentIdentifier = parentIdentifier.substring(1, parentIdentifier.length()-1);
        }
        Set<Triple<String, String, String>> annotations = dataset.getAnnotations(parentIdentifier);
        if(! annotations.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();

            if(! parent.has("@annotation")) {
                parent.put("annotation", "hh");
            }
            // ObjectNode annotationsObject = (ObjectNode) parent.get("annotation");



            log.info("%s: %s - %s".formatted(parent.get("@id"), fieldName, value.toString()));
        }

        return result;

    }



    private ArrayNode xexpandWithAnnotations(ArrayNode valueArray, ExtendedRdfDataset dataset) {
        ArrayNode result = valueArray.deepCopy();

        valueArray.forEach(item -> {
            if(item.isObject()) {

                if(item.has("@graph") && item.get("@graph").isArray()) {
                    result.addAll(this.expandWithAnnotations(item.withArray("@graph"), dataset));
                } else if(item.has("@id")) {
                    item.properties().forEach(property -> {
                        // get modifiable copy in result array
                        JsonNode mutableParent = Streams.stream(result).filter(jsonNode -> jsonNode.equals(item)).findFirst().orElseThrow();

                        if(property.getValue().isArray()) {
                            result.addAll(this.expandArrayWithAnnotations(item.withArray(property.getKey()), property.getKey(), (ObjectNode) mutableParent, dataset));
                        } else if(property.getValue().isObject()) {
                            result.add(this.expandObjectWithAnnotations(item.withObject(property.getKey()), property.getKey(), (ObjectNode) mutableParent, dataset));
                        }
                    });
                }
            }
            else if(item.isArray()) {
               result.addAll(this.expandWithAnnotations((ArrayNode) item, dataset));
            }
        });

        return result;
    }

    private ArrayNode xexpandArrayWithAnnotations(ArrayNode arrayNode, String fieldName, ObjectNode parent, ExtendedRdfDataset dataset) {
        ArrayNode result = arrayNode.deepCopy();

        arrayNode.forEach(node -> {
            if(node.isObject()) {
                result.add(this.expandObjectWithAnnotations((ObjectNode) node, fieldName, parent, dataset));
            }
        });
        return result;
    }

    private ObjectNode xexpandObjectWithAnnotations(ObjectNode value, String fieldName, ObjectNode parent, ExtendedRdfDataset dataset) {
        ObjectNode result = value.deepCopy();

        if(fieldName.equalsIgnoreCase("@id")) return result;

        String parentIdentifier = parent.get("@id").toString();
        // for some reason the identifier is double quoted, we have to remove them
        if(parentIdentifier.startsWith("\"") && parentIdentifier.endsWith(("\""))) {
            parentIdentifier = parentIdentifier.substring(1, parentIdentifier.length()-1);
        }
        Set<Triple<String, String, String>> annotations = dataset.getAnnotations(parentIdentifier);
        if(! annotations.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();

            if(! parent.has("@annotation")) {
                 parent.put("annotation", "hh");
            }
            // ObjectNode annotationsObject = (ObjectNode) parent.get("annotation");



            log.info("%s: %s - %s".formatted(parent.get("@id"), fieldName, value.toString()));
        }

        return result;

    }
*/

    private void expandWithAnnotations(JsonNode jsonObject, ExtendedRdfDataset t1) {
        log.info(jsonObject.toString());
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
