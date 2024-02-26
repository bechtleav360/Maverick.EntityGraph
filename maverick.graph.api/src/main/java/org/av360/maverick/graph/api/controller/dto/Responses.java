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

package org.av360.maverick.graph.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class Responses {

    public record EntityResponse(String identifier, String scope, String key, Set<String> type, Map<String, String> metadata, Set<Responses.ValueObject> values, Set<Responses.RelationObject> relations) { }


    @Schema(
            example = """
                    [
                        {
                            "property: "https://schema.org/audience",
                            "id": "...",
                            "details: [
                                "property": "urn:pwid:annot:source",
                                "value": "mistral"
                            ]
                        }
                    ]
                    """
    )
    public record RelationObject(String property, String id, Map<String, Serializable> metadata, Map<String, Serializable> details) { }

    @Schema(
            example = """
                    [
                        {
                            "property: "https://schema.org/description",
                            "value": "...",
                            "details: [
                                "property": "urn:pwid:annot:source",
                                "value": "mistral"
                            ]
                        }
                    ]
                    """
    )
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ValueObject(String property, Serializable value, String language, Map<String, Serializable> metadata, Map<String, Serializable> details) {
    }


    @Schema(
            example = """
                    [
                        {
                            "property": "urn:pwid:annot:source",
                            "value": "mistral"
                        }
                    ]
                    """
    )
    public record Detail(String property, String value) {
    }
}
