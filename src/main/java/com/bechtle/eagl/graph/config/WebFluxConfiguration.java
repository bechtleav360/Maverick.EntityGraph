package com.bechtle.eagl.graph.config;

import com.bechtle.eagl.graph.config.converter.decoder.StatementsDecoder;
import com.bechtle.eagl.graph.config.converter.encoder.BufferedStatementsEncoder;
import com.bechtle.eagl.graph.config.converter.encoder.StatementsEncoder;
import com.bechtle.eagl.graph.config.converter.encoder.TupleQueryResultsEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {

    public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
        builder.headerResolver();
        builder.parameterResolver().parameterName("format");

    }



    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // configurer.registerDefaults(false);
        configurer.customCodecs().register(new BufferedStatementsEncoder());
        configurer.customCodecs().register(new StatementsEncoder());
        configurer.customCodecs().register(new TupleQueryResultsEncoder());

        configurer.customCodecs().register(new StatementsDecoder());

        // configurer.customCodecs().register(new LinkedHashMapEncoder());

    }




}
