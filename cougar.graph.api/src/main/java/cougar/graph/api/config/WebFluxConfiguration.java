package cougar.graph.api.config;


import cougar.graph.api.converter.decoder.StatementsDecoder;
import cougar.graph.api.converter.encoder.BindingSetEncoder;
import cougar.graph.api.converter.encoder.BufferedStatementsEncoder;
import cougar.graph.api.converter.encoder.StatementsEncoder;
import cougar.graph.api.converter.encoder.TupleQueryResultsEncoder;
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
