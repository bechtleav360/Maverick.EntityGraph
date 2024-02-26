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
                .add(route(GET("/"), req ->
                        ServerResponse.temporaryRedirect(URI.create("/nav")).build())
                )
                .add(route(GET("/nav/entities"), req ->
                        ServerResponse.temporaryRedirect(URI.create("/nav/s/default/entities")).build())
                )
                .add(route(GET("/nav/s/{scope}/"), req ->
                        ServerResponse.temporaryRedirect(req.uriBuilder().replacePath("/nav/s/{scope}/entities").build(req.pathVariable("scope"))).build())
                )
                .add(route(GET("/nav/s/{scope}/entities/"), req ->
                        ServerResponse.temporaryRedirect(req.uriBuilder().replacePath("/nav/s/{scope}/entities").build(req.pathVariable("scope"))).build())
                )
                .add(route(GET("/nav/"), req -> ServerResponse.temporaryRedirect(URI.create("/nav")).build()))
                .build();
    }

}
