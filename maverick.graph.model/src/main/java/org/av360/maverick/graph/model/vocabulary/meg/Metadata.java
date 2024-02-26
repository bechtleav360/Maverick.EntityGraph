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

package org.av360.maverick.graph.model.vocabulary.meg;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

public class Metadata {

    public static final String NAMESPACE = "https://w3id.org/meg/emv/";
    public static final String PREFIX = "emv";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);


    /* Last update date */
    public static final IRI MODIFIED = LocalIRI.from(NAMESPACE, "modifiedAt");

    /* Creation date */
    public static final IRI CREATED = LocalIRI.from(NAMESPACE, "createdAt");


    /* Hash identifier */
    public static final IRI HASH_IDENTIFIER = LocalIRI.from(NAMESPACE, "hash");
}
