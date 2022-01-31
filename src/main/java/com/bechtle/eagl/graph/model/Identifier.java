package com.bechtle.eagl.graph.model;

import java.io.Serializable;

public interface Identifier extends Serializable {

    String getLocalName();
    String getNamespace();

}
