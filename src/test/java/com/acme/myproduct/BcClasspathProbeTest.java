package com.acme.myproduct;

import java.security.Provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BcClasspathProbeTest {

    @Test
    public void whichBouncyCastleProviderWins() throws Exception {
        Class<?> c = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
        String location = c.getProtectionDomain().getCodeSource().getLocation().toString();
        System.out.println("[BC-PROBE] class loaded from: " + location);

        Provider provider = (Provider) c.getDeclaredConstructor().newInstance();
        System.out.println("[BC-PROBE] provider getVersion(): " + provider.getVersion());

        // The winning BouncyCastleProvider is the copy shaded inside tika-app, NOT any
        // standalone bcprov-* jar in the dependency tree. If this assertion ever fails,
        // the classpath ordering changed and a different BouncyCastle edition now wins.
        assertTrue(location.contains("tika-app"),
                "expected BouncyCastleProvider to load from tika-app, but was: " + location);
        assertEquals(1.76, provider.getVersion(), 0.0001);
    }
}
