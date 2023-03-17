package io.av360.maverick.graph.api.config;


import io.av360.maverick.graph.api.converter.decoder.StatementsDecoder;
import io.av360.maverick.graph.api.converter.encoder.BindingSetEncoder;
import io.av360.maverick.graph.api.converter.encoder.BufferedStatementsEncoder;
import io.av360.maverick.graph.api.converter.encoder.StatementsEncoder;
import io.av360.maverick.graph.api.converter.encoder.TupleQueryResultsEncoder;
import io.av360.maverick.graph.services.SchemaServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {

    private final SchemaServices schemaServices;

    public WebFluxConfiguration(@Autowired SchemaServices schemaServices) {
        this.schemaServices = schemaServices;
    }

    public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
        builder.headerResolver();
        builder.parameterResolver().parameterName("format");
    }


    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new BufferedStatementsEncoder(this.schemaServices));
        configurer.customCodecs().register(new StatementsEncoder());
        configurer.customCodecs().register(new TupleQueryResultsEncoder());
        configurer.customCodecs().register(new BindingSetEncoder());
        configurer.customCodecs().register(new StatementsDecoder());
    }


    @Bean
    public WebFilter configureRequestFilter() {
        return (exchange,  chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(ReactiveRequestContextHolder.CONTEXT_KEY, request));
        };
    }


}
