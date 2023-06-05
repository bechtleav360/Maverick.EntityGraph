package org.av360.maverick.graph.model.util;

import org.springframework.web.server.WebFilter;

/**
 * Webfilter which are triggered before the authentication managers kick in
 */
public interface PreAuthenticationWebFilter extends WebFilter {
}
