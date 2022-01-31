package com.bechtle.eagl.graph.config;

import com.bechtle.eagl.graph.config.converter.RdfDecoder;
import com.bechtle.eagl.graph.connector.rdf4j.model.MutableValueFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {

    public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
        builder.parameterResolver().parameterName("format");

    }


    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new RdfDecoder());
    }




}
