package com.acme.myproduct;

import java.security.CodeSource;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaeeApiProbeTest {

    @Test
    public void whichJarProvidesEachJavaxApi() throws Exception {
        // Expected winning jar for each javax.* API, as observed by the probe. Several
        // packages are shadowed by more-specific jars (or the JDK), so only the ones
        // mapped to "javaee-api" actually exercise javaee-api-8.0.jar at runtime.
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("javax.json.Json", "javaee-api");
        expected.put("javax.json.bind.Jsonb", "javaee-api");
        // Step 8 (Finchley): the old jsr311-api (JAX-RS 1.1) that Edgware's ribbon/jersey stack
        // dragged is gone from the tree, so javax.ws.rs.core.Response now resolves to javaee-api-8.0.
        expected.put("javax.ws.rs.core.Response", "javaee-api");
        // Step 12 (Boot 2.3): spring-boot-starter-web no longer pulls Bean Validation transitively
        // (the validation-starter split). On 2.2 javax.validation.Validation was served by the
        // jakarta.validation-api:2.0.2 jar the web starter dragged in; on 2.3 that jar (and
        // hibernate-validator) leave the tree, so the class now resolves to the javaee-api:8.0 API
        // stubs. The app uses no @Valid / javax.validation anywhere, so losing the Hibernate Validator
        // runtime is a non-event (every context still starts) — a pure provenance flip, not a
        // regression. Re-pinned; NOT re-adding spring-boot-starter-validation for an unused API.
        expected.put("javax.validation.Validation", "javaee-api");   // was "validation-api"
        expected.put("javax.persistence.EnumType", "javaee-api");
        expected.put("javax.enterprise.inject.spi.CDI", "javaee-api");
        expected.put("javax.ejb.Stateless", "javaee-api");
        expected.put("javax.batch.api.Batchlet", "javaee-api");
        expected.put("javax.faces.context.FacesContext", "javaee-api");
        // Step 10 (Java 11 / JEP 320): the java.xml.ws.annotation module left the JDK, so the JSR-250
        // javax.annotation.* annotations are no longer served by the bootstrap classloader. The
        // javax.annotation-api jar (a transitive that was shadowed by rt.jar on Java 8) is now the
        // live provider. Re-pinned from "JDK/bootstrap" to the jar.
        // Step 11 (Boot 2.2): the BOM swapped the managed JSR-250 annotation API from
        // javax.annotation:javax.annotation-api to jakarta.annotation:jakarta.annotation-api:1.3.5 — a
        // renamed groupId that STILL ships the javax.annotation.* packages (Jakarta EE 8; the package
        // rename is a 3.0/jakarta-namespace concern, not here). Same class, new provenance jar. Re-pinned.
        expected.put("javax.annotation.Resource", "jakarta.annotation-api");
        expected.put("javax.mail.internet.InternetAddress", "javaee-api");
        expected.put("javax.servlet.Servlet", "javaee-api");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String name = entry.getKey();
            String marker = entry.getValue();

            Class<?> c = Class.forName(name);
            CodeSource src = c.getProtectionDomain().getCodeSource();
            String location = (src == null) ? "JDK/bootstrap" : src.getLocation().toString();
            System.out.println("[EE-PROBE] " + name + " <- " + location);

            if ("JDK/bootstrap".equals(marker)) {
                assertNull(src, name + " should be served by the JDK/bootstrap classloader");
            } else {
                assertNotNull(src, name + " should be on the classpath");
                assertTrue(location.contains(marker),
                        name + " expected from " + marker + " but was: " + location);
            }
        }
    }
}
