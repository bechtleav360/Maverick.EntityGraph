package com.bechtle.eagl.graph.features.multitenancy.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

public class Requests {

    public record RegisterApplicationRequest(String label, boolean persistent) {
    }

    public record CreateApiKeyRequest(String label) {}

}
