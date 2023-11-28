package org.av360.maverick.graph.feature.navigation.config;

import org.av360.maverick.graph.feature.navigation.controller.encoder.RdfHtmlEncoder;
import org.av360.maverick.graph.store.SchemaStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
public class FeatureConfiguration implements WebFluxConfigurer {


    private final SchemaStore schemaStore;

    public FeatureConfiguration(SchemaStore schemaStore) {
        this.schemaStore = schemaStore;
    }

    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("JSESSIONID");
        resolver.addCookieInitializer((builder) -> builder.path("/"));
        resolver.addCookieInitializer((builder) -> builder.sameSite("Strict"));
        return resolver;
    }

    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new RdfHtmlEncoder(schemaStore));
    }
}
