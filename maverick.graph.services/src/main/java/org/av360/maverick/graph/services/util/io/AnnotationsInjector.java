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

import com.apicatalog.jsonld.json.JsonProvider;
import jakarta.json.*;

import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationsInjector {

    public JsonArrayBuilder expandWithAnnotations(JsonArray valueArray, ExtendedRdfDataset dataset) {
        if(dataset.getAnnotations().isEmpty()) {
            return JsonProvider.instance().createArrayBuilder(valueArray);
        }

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

    public JsonObjectBuilder expandObjectWithAnnotations(JsonObject jsonObject, ExtendedRdfDataset dataset) {
        if(dataset.getAnnotations().isEmpty()) {
            return JsonProvider.instance().createObjectBuilder(jsonObject);
        }

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

}
