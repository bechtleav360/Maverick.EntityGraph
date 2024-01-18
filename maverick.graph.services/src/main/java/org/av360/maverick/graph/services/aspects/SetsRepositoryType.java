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

package org.av360.maverick.graph.services.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
@ConfigurationProperties
public class SetsRepositoryType {


    public record DefaultStorageConfiguration(String path, boolean persistence) {}


    @Value("${application.storage.entities.path:#{null}}")
    String entities_path;

    @Value("${application.storage.entities.persistent:false}")
    boolean entities_persistence;

    @Value("${application.storage.vocabularies.path:#{null}}")
    String vocabularies_path;

    @Value("${application.storage.vocabularies.persistent:false}")
    boolean vocabularies_persistence;

    @Value("${application.storage.transactions.path:#{null}}")
    String transactions_path;

    @Value("${application.storage.transactions.persistent:false}")
    boolean transactions_persistence;

    @Value("${application.storage.system.path:#{null}}")
    String system_path;

    @Value("${application.storage.system.persistent:false}")
    boolean system_persistence;

    @Around("@annotation(org.av360.maverick.graph.model.annotations.OnRepositoryType)")
    public Object addRepositoryTypeToEnvironment(ProceedingJoinPoint joinPoint) throws Throwable {

        Optional<SessionContext> sessionContextOptional = Arrays.stream(joinPoint.getArgs()).filter(o -> o instanceof SessionContext).findFirst().map(obk -> (SessionContext) obk);
        if(sessionContextOptional.isEmpty()) throw new IllegalArgumentException("Missing Session Context while resolving environment");
        SessionContext ctx = sessionContextOptional.get();


        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RepositoryType onRepositoryType = signature.getMethod().getAnnotation(OnRepositoryType.class).value();

        if (Objects.isNull(onRepositoryType)) {
            throw new UnsupportedOperationException("Missing target repository type while accessing a repository.");
        }

        if (RepositoryType.UNSET == onRepositoryType) {
            throw new UnsupportedOperationException("Missing target repository type while accessing a repository.");
        }

        ctx.getEnvironment().withRepositoryType(onRepositoryType);

        // add default configurations
        this.setDefaultConfigurations(ctx.getEnvironment());

        return joinPoint.proceed();
    }

    private void setDefaultConfigurations(Environment environment) {

        switch (environment.getRepositoryType()) {
            case APPLICATION:
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, this.system_persistence);
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, false);
                break;
            case ENTITIES:
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, this.entities_persistence);
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, false);
                break;
            case TRANSACTIONS:
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, this.transactions_persistence);
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, false);
                break;
            case SCHEMA:
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, this.vocabularies_persistence);
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, true);
                break;
            case UNSET:
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, false);
                environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, true);
        }
    }
}
