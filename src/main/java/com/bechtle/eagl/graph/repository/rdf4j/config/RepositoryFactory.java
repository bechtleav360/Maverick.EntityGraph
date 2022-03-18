package com.bechtle.eagl.graph.repository.rdf4j.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class RepositoryFactory  {
    private ObjectProvider<Repository> provider;


    @Autowired
    public void setProvider(ObjectProvider<Repository> provider) {
        this.provider = provider;
    }

    /**
     * Initializes the connection to a repository. The repository are cached
     *
     * @param repositoryType
     * @return
     * @throws IOException
     */
    public Repository getRepository(RepositoryConfiguration.RepositoryType repositoryType) throws IOException {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return this.provider.getObject(repositoryType, authentication);
    }
}
