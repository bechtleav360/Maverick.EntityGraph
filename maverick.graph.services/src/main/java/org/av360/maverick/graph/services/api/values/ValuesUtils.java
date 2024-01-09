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

package org.av360.maverick.graph.services.api.values;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.LanguageHandlerRegistry;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ValuesUtils {

    /**
     * Extracts the language tag.
     * Rules:
     * - tag in request parameter > tag in value
     * - default lang tag is "en"
     * - if one is invalid, we take the other (and write a warning into the logs)
     *
     * @param value    the literal value as string
     * @param paramTag the request parameter for the language tag, can be null
     * @return the identified language tag
     */
    public static Mono<Value> extractLanguageTag(String value, @Nullable String paramTag) {
        return Mono.create(sink -> {
            LanguageHandler languageHandler = LanguageHandlerRegistry.getInstance().get(LanguageHandler.BCP47).orElseThrow();
            SimpleValueFactory svf = SimpleValueFactory.getInstance();

            String valueTag = value.matches(".*@\\w\\w-?[\\w\\d-]*$") ? value.substring(value.lastIndexOf('@') + 1) : "";
            String strippedValue = StringUtils.isNotBlank(valueTag) ? value.substring(0, value.lastIndexOf('@')) : value;

            if (StringUtils.isNotBlank(paramTag)) {
                if (languageHandler.isRecognizedLanguage(paramTag) && languageHandler.verifyLanguage(value, paramTag)) {
                    sink.success(languageHandler.normalizeLanguage(strippedValue, paramTag, svf));
                } else {
                    sink.error(new IOException("Invalid language tag in parameter: " + paramTag));
                }
            } else if (StringUtils.isNotBlank(valueTag)) {
                if (languageHandler.isRecognizedLanguage(valueTag) && languageHandler.verifyLanguage(value, valueTag)) {
                    sink.success(languageHandler.normalizeLanguage(strippedValue, valueTag, svf));
                } else {
                    sink.error(new IOException("Invalid language tag in value: " + valueTag));
                }
            } else {
                sink.success(languageHandler.normalizeLanguage(strippedValue, "en", svf));
            }

        });
    }

    public static String generateHashForValue(IRI predicate, Value value) {
        return generateHashForValue(predicate.stringValue(), value.stringValue());
    }

    public static String generateHashForValue(String predicate, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((predicate+value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }



            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

}
