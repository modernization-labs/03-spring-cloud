package com.acme.myproduct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterizes the live API-documentation surface (SwaggerConfig). Pins the one load-bearing fact a
 * documentation surface owes the app: a descriptor is served and carries the configured API title.
 *
 * <p>Written against springfox's {@code /v2/api-docs} on the Boot 2.5 baseline so it goes green BEFORE
 * Step 16 moves any coordinate. The migration to springdoc-openapi then deliberately flips the asserted
 * path {@code /v2/api-docs} -> {@code /v3/api-docs} (a predicted flip -- the net doing its job), while the
 * "carries the title 'My Product API'" assertion is invariant: both springfox's {@code ApiInfo} and
 * springdoc's {@code OpenAPI.info} emit it.
 *
 * <p>Jupiter, not JUnit 4: the net was migrated to JUnit 5 (the JUnit 4 API is not on the classpath).
 * Basic auth + deterministic {@code spring.security.user.*}: the app secures every path (unauthenticated
 * access -> 302 /login, see OAuth2SecurityCharacterizationTest), and {@code /v2/api-docs} sits behind that
 * same wall -- so the descriptor is fetched WITH credentials, exactly as ActuatorEndpointCharacterizationTest
 * fetches {@code /actuator/health}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.security.user.name=probe",
        "spring.security.user.password=probe-pw"
})
public class OpenApiDocsCharacterizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void apiDocsDescriptorIsServedAndCarriesTheConfiguredTitle() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                //.getForEntity("/v2/api-docs", String.class); -> /v3/api-docs after the springdoc migration
                .getForEntity("/v3/api-docs", String.class);         // springdoc
        assertEquals(200, response.getStatusCodeValue(),
                "expected the OpenAPI descriptor to be served (200), but was: " + response.getStatusCode());
        assertTrue(response.getBody().contains("My Product API"),
                "expected the descriptor to carry the configured title, but was: " + response.getBody());
    }
}
