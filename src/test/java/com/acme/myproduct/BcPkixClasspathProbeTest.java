package com.acme.myproduct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Companion to {@link BcClasspathProbeTest}. That probe pinned exactly ONE class
 * (the JCE provider, org.bouncycastle.jce.provider.BouncyCastleProvider), and proved it
 * loads from tika-app's shaded copy. But BouncyCastle is not one jar: bcprov (the provider
 * + low-level crypto), bcpkix (cert/CMS/operator/tsp), and bcmail (S/MIME) are separate
 * modules, each present in MULTIPLE editions on this classpath (jdk14:1.38, jdk15on:1.56/1.57,
 * jdk18on:1.76, plus a shaded copy inside tika-app). Whether a future "BouncyCastle
 * rationalization" step is safe depends on where the NON-provider classes actually load from.
 *
 * This probe reports the code source of one representative class per BC package, so we can
 * tell — per module — whether the live class comes from tika-app's shaded copy (safe to touch
 * the standalone jars) or from a standalone bcpkix/bcmail jar that iText / spring-security-jwt
 * actually use (touching the standalone jars is then a real behaviour change).
 *
 * It is a characterization probe: the assertions pin the CURRENT winners so a later step that
 * shifts classpath ordering is forced to surface the change instead of absorbing it silently.
 */
public class BcPkixClasspathProbeTest {

    private static String locationOf(String fqcn) {
        try {
            Class<?> c = Class.forName(fqcn);
            return c.getProtectionDomain().getCodeSource().getLocation().toString();
        } catch (ClassNotFoundException e) {
            return "<not on classpath>";
        } catch (NullPointerException e) {
            // bootstrap / no code source
            return "<no code source>";
        }
    }

    @Test
    public void whereDoTheNonProviderBouncyCastleClassesLoadFrom() {
        String[] probes = {
                "org.bouncycastle.jce.provider.BouncyCastleProvider", // bcprov  (baseline, known: tika-app)
                "org.bouncycastle.cert.X509CertificateHolder",        // bcpkix  (cert)
                "org.bouncycastle.cms.CMSSignedData",                 // bcpkix  (cms)
                "org.bouncycastle.operator.ContentSigner",            // bcpkix  (operator)
                "org.bouncycastle.tsp.TimeStampToken",                // bcpkix(jdk15on)/bctsp(jdk14)
                "org.bouncycastle.mail.smime.SMIMESigned",            // bcmail  (S/MIME)
                "org.bouncycastle.openssl.PEMParser",                 // bcpkix  (openssl/PEM)
                "org.bouncycastle.util.encoders.Hex",                 // bcprov/bcutil (low-level util)
        };

        for (String fqcn : probes) {
            System.out.println("[BCPKIX-PROBE] " + fqcn + " -> " + locationOf(fqcn));
        }

        // FINDING (see README "## bcpkix probe"): BouncyCastle on this classpath is SPLIT.
        // The provider + low-level bcprov/bcutil + bcmail S/MIME classes are dominated by
        // tika-app's shaded copy (1.76) -- like the provider in BcClasspathProbeTest. BUT the
        // entire bcpkix module (cert / cms / operator / tsp / openssl) loads LIVE from the
        // standalone bcpkix-jdk15on:1.56 jar that spring-security-jwt drags in -- it is NOT
        // shadowed by tika-app. That jar is therefore load-bearing, not dead weight: a BC
        // "rationalization" step that touches it is a real behaviour change, unlike the log4j2
        // align. If any assertion below flips, classpath ordering moved -- investigate, do not
        // silently absorb it.
        assertLoadsFrom("org.bouncycastle.jce.provider.BouncyCastleProvider", "tika-app");
        assertLoadsFrom("org.bouncycastle.util.encoders.Hex", "tika-app");
        assertLoadsFrom("org.bouncycastle.mail.smime.SMIMESigned", "tika-app");
        assertLoadsFrom("org.bouncycastle.cert.X509CertificateHolder", "bcpkix-jdk15on");
        assertLoadsFrom("org.bouncycastle.cms.CMSSignedData", "bcpkix-jdk15on");
        assertLoadsFrom("org.bouncycastle.operator.ContentSigner", "bcpkix-jdk15on");
        assertLoadsFrom("org.bouncycastle.tsp.TimeStampToken", "bcpkix-jdk15on");
        assertLoadsFrom("org.bouncycastle.openssl.PEMParser", "bcpkix-jdk15on");
    }

    private static void assertLoadsFrom(String fqcn, String jarMarker) {
        String location = locationOf(fqcn);
        assertTrue(location.contains(jarMarker), "expected " + fqcn + " to load from a jar containing '" + jarMarker
                + "', but was: " + location);
    }
}
