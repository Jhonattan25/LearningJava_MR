package com.learning.webflux.app.controllers;

import com.learning.webflux.app.handler.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterFunctionConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler productHandler) {
        return route(GET("/api/v2/products").or(GET("/api/v3/products")), productHandler::list)
                .andRoute(GET("/api/v2/products/{id}"), productHandler::show)
                .andRoute(POST("/api/v2/products"), productHandler::create)
                .andRoute(PUT("/api/v2/products/{id}"), productHandler::update)
                .andRoute(DELETE("/api/v2/products/{id}"), productHandler::delete);
    }
}
