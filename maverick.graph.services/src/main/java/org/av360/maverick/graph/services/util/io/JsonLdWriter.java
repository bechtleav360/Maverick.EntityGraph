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

package org.av360.maverick.graph.services.util.io;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.json.JsonProvider;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Statement;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Utility class which converts a Collection of RDF Statements into a compacted Json LD document
 */
@Slf4j
public class JsonLdWriter {


    public static ExtendedRdfDataset convertModelToDataset(Set<Statement> statements) {
        return statements.stream().collect(RdfDatasetCollector.toDataset());
    }

    public static Mono<String> serializeJsonValue(JsonValue object) {
        JsonWriterFactory writerFactory = JsonProvider.instance().createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));
        try (StringWriter stringWriter = new StringWriter(); JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
            jsonWriter.write(object); // Write JSON object to writer
            return Mono.just(stringWriter.toString());
        } catch (IOException e) {
            return Mono.error(e);
        }
    }



    public static Mono<JsonObject> buildJsonObject(Tuple2<ExtendedRdfDataset, URI> tuple) {
        return buildJsonObject(tuple.getT1(), tuple.getT2());
    }

    public static Mono<JsonObject> buildJsonObject(ExtendedRdfDataset dataset, URI contextURI) {
        JsonLdOptions options = new JsonLdOptions();
        options.setCompactArrays(true);
        options.setOmitGraph(true);
        options.setRdfStar(true);
        // options.setUseRdfType(true);
        // options.setEmbed(JsonLdEmbed.ALWAYS);

        try {
            Document document = RdfDocument.of(dataset);
            JsonArray jsonValues = JsonLd.fromRdf(document).options(options).get();


            // add annotations by hand
            jsonValues = new AnnotationsInjector().expandWithAnnotations(jsonValues, dataset).build();

            // serializeJsonValue(jsonValues).doOnNext(log::info).subscribe();

            JsonDocument jsonDocument;

            if(jsonValues.size() == 1) {
                jsonDocument = JsonDocument.of(jsonValues.get(0).asJsonObject());
            } else {
                jsonDocument = JsonDocument.of(jsonValues);
            }

            JsonObject jsonObject = JsonLd.frame(jsonDocument, contextURI).options(options).get();

            return Mono.just(jsonObject);
        } catch (JsonLdError e) {
            return Mono.error(e);
        }
    }



}
