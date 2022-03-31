package com.bechtle.eagl.graph.repository.rdf4j.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Deprecated
public class RepositoryProvider implements ObjectProvider<Repository> {
    @Override
    public Repository getObject(Object... objects) throws BeansException {
        log.info("Initializing Repository");
        return null;
    }

    @Override
    public Repository getIfAvailable() throws BeansException {
        return null;
    }

    @Override
    public Repository getIfUnique() throws BeansException {
        return null;
    }

    @Override
    public Repository getObject() throws BeansException {
        return null;
    }
}
