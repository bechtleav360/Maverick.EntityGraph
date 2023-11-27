/*
 * Copyright (c) 2023.
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

package org.av360.maverick.graph.services.preprocessors.validateRequiredType;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.errors.runtime.MissingType;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.preprocessors.ModelPreprocessor;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@Slf4j(topic = "graph.srvc.validator.type")
@Component
@ConditionalOnProperty(name = "application.features.validators.checkRequiredType", havingValue = "true")
public class CheckRequiredType implements ModelPreprocessor {


    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public Mono<? extends Model> handle(Model model, Map<String, String> parameters, Environment environment) {
        log.trace("Checking if type is defined");


        for (Resource subj : model.subjects()) {
            /* we only require type definitions for individuals and classifiers */
            Set<Value> types = model.filter(subj, RDF.TYPE, null).objects();

            if (types.size() == 0) {
                log.warn("The fragment with subject [{}] is missing a type definition.", subj);
                return Mono.error(new MissingType(subj));
            }

            if (types.contains(Local.Entities.TYPE_INDIVIDUAL) && types.size() < 2) {
                log.warn("The individual with subject [{}] is missing a type definition.", subj);
                return Mono.error(new MissingType(subj));
            }
            if (types.contains(Local.Entities.TYPE_CLASSIFIER) && types.size() < 2) {
                log.warn("The individual with subject [{}] is missing a type definition.", subj);
                return Mono.error(new MissingType(subj));
            }


        }
        return Mono.just(model);
    }
}
