package com.acme.myproduct;

import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes which SLF4J backend actually wins — a sibling to {@link BcClasspathProbeTest}.
 *
 * <p>The dependency tree is misleading here. It lists logback-classic 1.1.11 (a real SLF4J 1.7
 * StaticLoggerBinder) and slf4j-api 1.7.25, which would suggest Logback. But tika-app 2.9.0 is a
 * shaded uber-jar that bundles, UNRELOCATED, a full SLF4J 2.x + log4j2 2.20.0 stack
 * (org/slf4j/LoggerFactory, the SLF4JServiceProvider service file, and log4j-core/api). tika-app
 * sorts early on the classpath, so its shaded org.slf4j.LoggerFactory (2.x, ServiceLoader-based)
 * shadows slf4j-api:1.7.25 and binds to log4j2 — NOT Logback.
 *
 * <p>So the live logging backend is log4j2 2.20.0 served from tika-app's shaded copy. The
 * standalone log4j-core/api jars in the tree are effectively dead for the active path, exactly like
 * the standalone bcprov-* jars in {@link BcClasspathProbeTest}. If this probe ever flips, classpath
 * ordering changed and a different backend now wins: investigate, do not silently absorb it.
 */
public class LoggingBackendProbeTest {

    @Test
    public void log4j2FromTikaAppIsTheActiveSlf4jBackend() throws Exception {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        String factoryName = factory.getClass().getName();
        String slf4jLocation = LoggerFactory.class.getProtectionDomain()
                .getCodeSource().getLocation().toString();
        String factoryLocation = factory.getClass().getProtectionDomain()
                .getCodeSource().getLocation().toString();

        System.out.println("[LOG-PROBE] active ILoggerFactory : " + factoryName);
        System.out.println("[LOG-PROBE] org.slf4j.LoggerFactory from : " + slf4jLocation);
        System.out.println("[LOG-PROBE] binding factory from        : " + factoryLocation);

        // The winner is log4j2's binding, and the org.slf4j.LoggerFactory that selected it is the
        // SLF4J 2.x copy shaded inside tika-app — not the declared slf4j-api:1.7.25.
        assertEquals("org.apache.logging.slf4j.Log4jLoggerFactory", factoryName);
        assertTrue("expected org.slf4j.LoggerFactory to load from tika-app's shaded copy, but was: "
                + slf4jLocation, slf4jLocation.contains("tika-app"));
    }
}
