package io.av360.maverick.graph.api.config;


import io.av360.maverick.graph.api.converter.decoder.StatementsDecoder;
import io.av360.maverick.graph.api.converter.encoder.BindingSetEncoder;
import io.av360.maverick.graph.api.converter.encoder.BufferedStatementsEncoder;
import io.av360.maverick.graph.api.converter.encoder.StatementsEncoder;
import io.av360.maverick.graph.api.converter.encoder.TupleQueryResultsEncoder;
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
        configurer.customCodecs().register(new BufferedStatementsEncoder());
        configurer.customCodecs().register(new StatementsEncoder());
        configurer.customCodecs().register(new TupleQueryResultsEncoder());
        configurer.customCodecs().register(new BindingSetEncoder());
        configurer.customCodecs().register(new StatementsDecoder());

    }




}
