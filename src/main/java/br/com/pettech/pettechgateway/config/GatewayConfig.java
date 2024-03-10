package br.com.pettech.pettechgateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    private final AuthenticationFilter filter;

    public GatewayConfig(AuthenticationFilter filter) {
        this.filter = filter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/produtos/**")
                        .filters(f -> f.filter(filter))
                        .uri("http://localhost:3001"))

                .route(r -> r.path("/auth/**")
                        .filters(f -> f.filter(filter))
                        .uri("http://localhost:3002"))
                .build();
    }

}
