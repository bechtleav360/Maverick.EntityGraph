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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.*;

/***
 * The JavaArrays coming from Titanium are immutable, we have to convert it into a modifiable data structure

 */
public class JsonJacksonConverter {


    private static ObjectMapper objectMapper;

    private static ObjectMapper getObjectMapper() {
        if(objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    public static ArrayNode deepCopyJsonArray(JsonArray originalArray) {
        ArrayNode arrayNode = getObjectMapper().createArrayNode();

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (JsonValue value : originalArray) {
            arrayNode.add(copyJsonValue(value));
        }
        return arrayNode;
    }

    private static JsonNode copyJsonValue(JsonValue jsonValue) {
        switch (jsonValue.getValueType()) {
            case ARRAY:
                return deepCopyJsonArray(jsonValue.asJsonArray());
            case OBJECT:
                return deepCopyJsonObject(jsonValue.asJsonObject());
            default:
                try {
                    return getObjectMapper().readTree(jsonValue.toString());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    private static ObjectNode deepCopyJsonObject(JsonObject originalObject) {
        ObjectNode objectNode = getObjectMapper().createObjectNode();

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        for (String key : originalObject.keySet()) {
            objectNode.set(key, copyJsonValue(originalObject.get(key)));
        }
        return objectNode;
    }
}
