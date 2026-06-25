package com.acme.myproduct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterizes the security wall around "/". The endpoint stays protected across the Boot 1.5 -> 2.0
 * hop, but Step 8 surfaced that 2.0 CHANGED the challenge mechanism, not just the autoconfig wiring:
 *
 *   - Boot 1.5 (spring-security-oauth2 auto-applied): no/wrong credentials -> 401 (HTTP-Basic challenge).
 *   - Boot 2.0 (Spring Security 5 default autoconfig): valid credentials still -> 200, but no/wrong
 *     credentials -> 302 redirect to /login (the form-login entry point wins for browser-like requests).
 *
 * The wall still holds (unauthenticated access is denied), so this is a contract-SHAPE change, not a
 * hole. A first draft assumed spring-security-oauth2-autoconfigure would restore the API-style 401; it
 * does not (the app uses no OAuth2 features, so the bridge is a no-op here). Restoring a 401 challenge
 * is part of the deliberately-deferred OAuth2 re-architecture off the EOL module, not this step.
 *
 * Deterministic credentials are pinned via spring.security.user.* (renamed from security.user.* in 2.0)
 * so the assertion does not depend on the random password printed at startup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Step 8 (Boot 2.0): the deterministic-user keys moved security.user.* -> spring.security.user.*
        "spring.security.user.name=probe",
        "spring.security.user.password=probe-pw"
})
public class OAuth2SecurityCharacterizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void rootWithoutCredentialsRedirectsToLogin() {
        // Boot 2.0: was 401 (Basic challenge) under 1.5; now 302 -> /login (form-login entry point).
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
    }

    @Test
    public void rootWithValidCredentialsIs200AndHelloWorld() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("hello world", response.getBody());
    }

    @Test
    public void rootWithWrongPasswordRedirectsToLogin() {
        // Boot 2.0: wrong Basic creds fall through to the form-login entry point -> 302 (was 401 in 1.5).
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "wrong")
                .getForEntity("/", String.class);
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
    }
}
