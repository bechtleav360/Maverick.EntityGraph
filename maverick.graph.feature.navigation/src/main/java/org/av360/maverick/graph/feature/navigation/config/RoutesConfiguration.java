package org.av360.maverick.graph.feature.navigation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RoutesConfiguration {


    @Bean
    RouterFunction<ServerResponse> redirectStart() {
        return route()
                .add(route(GET("/"), req -> ServerResponse.temporaryRedirect(URI.create("/nav")).build()))
                .add(route(GET("/nav/"), req -> ServerResponse.temporaryRedirect(URI.create("/nav")).build()))
                .build();
    }

}
