package com.acme.myproduct;

import java.security.CodeSource;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JavaeeApiProbeTest {

    @Test
    public void whichJarProvidesEachJavaxApi() throws Exception {
        // Expected winning jar for each javax.* API, as observed by the probe. Several
        // packages are shadowed by more-specific jars (or the JDK), so only the ones
        // mapped to "javaee-api" actually exercise javaee-api-8.0.jar at runtime.
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("javax.json.Json", "javaee-api");
        expected.put("javax.json.bind.Jsonb", "javaee-api");
        expected.put("javax.ws.rs.core.Response", "jsr311-api");
        expected.put("javax.validation.Validation", "validation-api");
        expected.put("javax.persistence.EnumType", "javaee-api");
        expected.put("javax.enterprise.inject.spi.CDI", "javaee-api");
        expected.put("javax.ejb.Stateless", "javaee-api");
        expected.put("javax.batch.api.Batchlet", "javaee-api");
        expected.put("javax.faces.context.FacesContext", "javaee-api");
        expected.put("javax.annotation.Resource", "JDK/bootstrap");
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
                assertNull(name + " should be served by the JDK/bootstrap classloader", src);
            } else {
                assertNotNull(name + " should be on the classpath", src);
                assertTrue(name + " expected from " + marker + " but was: " + location,
                        location.contains(marker));
            }
        }
    }
}
