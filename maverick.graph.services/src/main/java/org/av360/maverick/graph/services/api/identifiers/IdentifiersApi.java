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

package org.av360.maverick.graph.services.api.identifiers;

import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.api.Api;

public class IdentifiersApi {
        private final Api parent;
    private final IdentifierServices identifierServices;

    public PrefixResolver prefixes() {
            return prefixServices;
        }

        private final PrefixResolver prefixServices;

        public LocalIdentifiers localIdentifiers() {
            return localIdentifierServices;
        }

        private final LocalIdentifiers localIdentifierServices;


        public IdentifiersApi(Api parent, IdentifierServices identifierServices) {
            this.parent = parent;
            this.identifierServices = identifierServices;
            this.prefixServices = new PrefixResolver(this.parent);
            this.localIdentifierServices = new LocalIdentifiers(this.parent, identifierServices);
        }
    }