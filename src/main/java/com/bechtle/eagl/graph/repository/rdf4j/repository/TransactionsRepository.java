package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.repository.TransactionsStore;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.beans.factory.annotation.Qualifier;

public class TransactionsRepository implements TransactionsStore {

    private final Repository repository;

    public TransactionsRepository(@Qualifier("transactions-storage") Repository repository) {
        this.repository = repository;
    }
}
