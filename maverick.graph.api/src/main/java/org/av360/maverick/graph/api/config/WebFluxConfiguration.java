package org.av360.maverick.graph.api.config;


import org.av360.maverick.graph.api.converter.decoder.BufferedStatementsDecoder;
import org.av360.maverick.graph.api.converter.decoder.StreamingStatementsDecoder;
import org.av360.maverick.graph.api.converter.encoder.BindingSetEncoder;
import org.av360.maverick.graph.api.converter.encoder.BufferedStatementsEncoder;
import org.av360.maverick.graph.api.converter.encoder.StatementsEncoder;
import org.av360.maverick.graph.api.converter.encoder.TupleQueryResultsEncoder;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.services.SchemaServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**")
                .allowedMethods("GET")
                .maxAge(3600);
    }

    private final SchemaServices schemaServices;
    private final Environment environment;

    public WebFluxConfiguration(@Autowired SchemaServices schemaServices, @Autowired Environment environment) {
        this.schemaServices = schemaServices;
        this.environment = environment;
    }

    public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
        builder.headerResolver();
        builder.parameterResolver().parameterName("format");
        //builder.fixedResolver(MediaType.parseMediaType(RDFFormat.JSONLD.getDefaultMIMEType()));  // setting a default content type interferes with swagger html client
    }


    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new BufferedStatementsEncoder(this.schemaServices, environment));
        configurer.customCodecs().register(new StatementsEncoder());
        configurer.customCodecs().register(new TupleQueryResultsEncoder());
        configurer.customCodecs().register(new BindingSetEncoder());
        configurer.customCodecs().register(new BufferedStatementsDecoder());
        configurer.customCodecs().register(new StreamingStatementsDecoder());
    }


    @Bean
    public WebFilter configureRequestFilter() {
        return (exchange,  chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            return chain.filter(exchange)
                    .contextWrite(ctx -> {
                        ctx = ctx.put(ReactiveRequestUriContextHolder.CONTEXT_URI_KEY, request.getURI());
                        ctx = ctx.put(ReactiveRequestUriContextHolder.CONTEXT_HEADERS_KEY, request.getHeaders());
                        return ctx;
                    });
        };
    }


    @Bean
    public Converter<String, RepositoryType> convertRepositoryEnum() {
        return new Converter<String, RepositoryType>() {
            @Override
            public RepositoryType convert(String source) {
                return RepositoryType.valueOf(source.toUpperCase());
            }
        };
    }




}
