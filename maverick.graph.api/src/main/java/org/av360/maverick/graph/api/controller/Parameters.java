package org.av360.maverick.graph.api.controller;

import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;

/**
 * a list of supported request parameters
 */
public class Parameters {

    public static final String HEADER_API_KEY = ApiKeyAuthenticationToken.API_KEY_HEADER;

    public static String PAGE = "page";
    public static String COUNT = "count";
    public static String FORCE_GENERATE_IDENTIFIER = "generate-identifier";

}
