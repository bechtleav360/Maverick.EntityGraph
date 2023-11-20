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

package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.store.InvalidStoreConfiguration;
import org.av360.maverick.graph.store.behaviours.*;
import org.slf4j.Logger;

public interface EntityStore {
    RepositoryType getRepositoryType();

    Logger getLogger();

    default Fragmentable asFragmentable() {
        if(this instanceof Fragmentable fragmentable) {
            return fragmentable;
        } else throw new InvalidStoreConfiguration("This store of type {} does not implement the behaviour: Fragmentable");
    }

    default Selectable asSelectable() {
        if(this instanceof Selectable selectable) {
            return selectable;
        } else throw new InvalidStoreConfiguration("This store of type {} does not implement the behaviour: Selectable");
    }

    default Searchable asSearchable() {
        if(this instanceof Searchable searchable) {
            return searchable;
        } else throw new InvalidStoreConfiguration("This store of type {} does not implement the behaviour: Searchable");
    }


    default Commitable asCommitable() {
        if(this instanceof Commitable commitable) {
            return commitable;
        } else throw new InvalidStoreConfiguration("This store of type {} does not implement the behaviour: Commitable");
    }

    default Maintainable asMaintainable() {
        if(this instanceof Maintainable maintainable) {
            return maintainable;
        } else throw new InvalidStoreConfiguration("This store of type {} does not implement the behaviour: Maintainable");
    }


    default StatementsAware asStatementsAware() {
        if(this instanceof StatementsAware statementsAware) {
            return statementsAware;
        } else throw new InvalidStoreConfiguration("This store of type {} does not implement the behaviour: StatementsAware");
    }

    default boolean isSearchable() {
        return this instanceof Searchable;
    }

    default boolean isMaintainable() {
        return this instanceof Maintainable;
    }
}
