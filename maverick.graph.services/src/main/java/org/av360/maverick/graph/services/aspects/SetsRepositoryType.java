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
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class SetsRepositoryType {

    @Around("@annotation(org.av360.maverick.graph.model.annotations.OnRepositoryType)")
    public Object addRepositoryTypeToEnvironment(ProceedingJoinPoint joinPoint) throws Throwable {

        Optional<SessionContext> sessionContextOptional = Arrays.stream(joinPoint.getArgs()).filter(o -> o instanceof SessionContext).findFirst().map(obk -> (SessionContext) obk);
        if(sessionContextOptional.isEmpty()) throw new IllegalArgumentException("Missing Session Context while running authorization");
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

        return joinPoint.proceed();
    }
}
