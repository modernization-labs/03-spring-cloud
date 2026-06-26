package com.acme.myproduct;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi configuration -- the live API-documentation surface of the service.
 *
 * <p>Step 16 replaced the abandoned springfox 2.7.0 wiring ({@code @EnableSwagger2} + {@code Docket}),
 * which broke on Boot 2.6's {@code PathPatternParser}-default path matching and circular-reference
 * prohibition. springdoc auto-configures the descriptor at {@code /v3/api-docs} and the UI at
 * {@code /swagger-ui.html}; this {@link OpenAPI} bean only supplies the {@link Info} (title, description,
 * version), preserving the documented title "My Product API" across the migration.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("My Product API")
                        .description("springdoc-openapi-generated OpenAPI 3 documentation for the my-product service.")
                        .version("1.0.0"));
    }
}
