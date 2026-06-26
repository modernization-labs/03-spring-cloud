package com.acme.myproduct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Springfox Swagger 2 configuration -- the live API-documentation surface of the service.
 *
 * <p>{@code @EnableSwagger2} registers springfox's {@code documentationPluginsBootstrapper}
 * (a {@code SmartLifecycle}) into the context, and the {@link Docket} bean below scans the
 * {@code com.acme.myproduct} package, publishing the Swagger 2 descriptor at
 * {@code /v2/api-docs} and the UI at {@code /swagger-ui.html}.
 *
 * <p>This is the classic springfox 2.x wiring carried over from the legacy stack. It is the
 * surface the modernization walk must preserve: springfox 2.7.0 is abandoned and breaks on
 * Boot 2.6's {@code PathPatternParser}-default path matching, so the planned Step 16 migrates
 * this configuration to {@code springdoc-openapi} while keeping the documented endpoints intact.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.acme.myproduct"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("My Product API")
                .description("Springfox-generated Swagger 2 documentation for the my-product service.")
                .version("1.0.0")
                .build();
    }
}
