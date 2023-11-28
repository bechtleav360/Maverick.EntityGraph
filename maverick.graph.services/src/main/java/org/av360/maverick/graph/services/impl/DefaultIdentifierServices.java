package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.services.IdentifierServices;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DefaultIdentifierServices implements IdentifierServices {

    public DefaultIdentifierServices() {

    }

    @Override
    public String validate(String identifier, Environment environment) {
        return identifier;
    }








}
