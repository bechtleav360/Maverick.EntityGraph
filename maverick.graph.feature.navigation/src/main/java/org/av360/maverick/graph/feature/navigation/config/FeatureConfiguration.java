package org.av360.maverick.graph.feature.navigation.config;

import org.av360.maverick.graph.feature.navigation.api.encoder.RdfHtmlEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class FeatureConfiguration implements WebFluxConfigurer {



    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new RdfHtmlEncoder());
    }
}
