package com.acme.myproduct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterizes the Actuator endpoint contract. Step 8 (Boot 1.5 -> 2.0) flipped these two facts
 * by design -- they are the only assertions the jump was expected to invert:
 *
 *   1. Health now lives under "/actuator/health" (Boot 2.0), not at the root "/health" (Boot 1.5).
 *   2. The root "/health" path is gone (404 when authenticated).
 *
 * The flip being confined to exactly these two assertions -- while the security and JAXB knots
 * stayed green -- is the proof the Boot 2.0 hop moved the Actuator paths and nothing else.
 *
 * NOTE on the status verdict: in this infra-free test environment the aggregate health is DOWN
 * (HTTP 503), because the amqp/MQ health indicators cannot reach a broker. That is expected and
 * deterministic here (the project runs with "no infra needed"). We therefore pin the PATH and
 * the response SHAPE (a JSON body carrying a "status" field), NOT the UP/DOWN verdict -- the
 * verdict is a property of the missing broker, whereas the path/shape is the 1.5 contract the
 * Boot 2.0 hop actually changes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Step 8 (Boot 2.0): deterministic-user keys moved security.user.* -> spring.security.user.*
        "spring.security.user.name=probe",
        "spring.security.user.password=probe-pw",
        // Boot 2.0 makes actuator endpoints opt-in over HTTP; expose health explicitly.
        "management.endpoints.web.exposure.include=health"
})
public class ActuatorEndpointCharacterizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void healthIsServedUnderActuatorPrefixWithStatusBody() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/actuator/health", String.class);

        // Boot 2.0: health now lives under /actuator/*. It responds (not 404); 200 (UP) or 503 (DOWN)
        // both prove the path is served; here it is 503 because no broker is reachable (see javadoc).
        assertTrue(response.getStatusCode() == HttpStatus.OK
                        || response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE,
                "expected /actuator/health to be served (200 or 503), but was: "
                        + response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":"),
                "expected a health JSON carrying a status field, but was: "
                        + response.getBody());
    }

    @Test
    public void rootHealthIsGoneIn20() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/health", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
