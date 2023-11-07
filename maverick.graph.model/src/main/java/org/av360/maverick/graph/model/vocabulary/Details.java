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

package org.av360.maverick.graph.model.vocabulary;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary for detail annotations
 */
public class Details {

    public static final String NAMESPACE = "http://w3id.org/eaf#";
    public static final String PREFIX = "eaf";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);


    /* The value identifier */
    public static final IRI HASH = LocalIRI.from(NAMESPACE, "hash");

    /** COUNTS **/

    /* Number of users who have applied the given annotation */
    public static final IRI USER_COUNT = LocalIRI.from(NAMESPACE, "userCount");

    /* Implicit user feedback counting the number of interactions, where a user followed a recommendation */
    public static final IRI ACKNOWLEDGE_COUNT = LocalIRI.from(NAMESPACE, "acknowledgeCount ");

    /* Explicit user feedback counting the number of interactions, where a user confirmed a recommendation*/
    public static final IRI CONFIRM_COUNT = LocalIRI.from(NAMESPACE, "confirmationCount");

    /* Explicit user feedback counting the number of interactions, where a user rejected a recommendation */
    public static final IRI REJECT_COUNT = LocalIRI.from(NAMESPACE, "rejectCount");

    /* Implicit user feedback counting the number of interactions, where a user ignored a recommendation */
    public static final IRI IGNORE_COUNT = LocalIRI.from(NAMESPACE, "ignoreCount");

    /** COMMENTS **/

    /* Explicit feedback by users as comments */
    public static final IRI USER_FEEDBACK = LocalIRI.from(NAMESPACE, "userFeedback");

    /** RECOMMENDATIONS **/

    /* Intended audience for a property */
    public static final IRI AUDIENCE = LocalIRI.from(NAMESPACE, "audience");

    public static final IRI CONFIDENCE = LocalIRI.from(NAMESPACE, "confidence");

    public static final IRI SOURCE = LocalIRI.from(NAMESPACE, "source");

    public static final IRI STORY = LocalIRI.from(NAMESPACE, "story");
}
