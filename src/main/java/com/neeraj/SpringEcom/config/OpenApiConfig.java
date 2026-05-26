package com.neeraj.SpringEcom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI springEcomOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpringEcom API")
                        .description("REST API documentation for SpringEcom ecommerce backend")
                        .version("0.0.1-SNAPSHOT"));
    }
}