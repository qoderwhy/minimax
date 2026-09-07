package com.qkit.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置（springdoc + knife4j）。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI qkitOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("qkit 脚手架 API")
                        .description("qkit 前后台脚手架接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("qkit Team"))
                        .license(new License().name("Apache 2.0")));
    }
}
