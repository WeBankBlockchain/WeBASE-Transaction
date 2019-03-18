package com.webank.webase.transaction.config;

import com.google.common.collect.Lists;
import java.util.HashSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    /**
     * documentation.
     * 
     * @return
     */
    @Bean
    public Docket documentation() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.webank.webase.transaction")).build()
                .protocols(new HashSet<String>(Lists.newArrayList("http"))).pathMapping("/")
                .apiInfo(apiInfo()).enable(true);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("API document").description("transaction api")
                .version("1.0").build();
    }
}
