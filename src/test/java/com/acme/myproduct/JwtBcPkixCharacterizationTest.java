package com.acme.myproduct;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Step 3 characterization test. Unlike the bcpkix *probe* (which pins WHICH jar serves the
 * bcpkix classes), this pins the BEHAVIOUR of the live bcpkix signing path the way the app's
 * JWT stack actually uses it: spring-security-jwt's RsaSigner parses a PEM private key via
 * spring-security-rsa's RsaKeyHelper, which calls org.bouncycastle.openssl.PEMParser -- the
 * class the bcpkix probe proved loads live from bcpkix-jdk15on:1.56. A future "rationalize
 * BouncyCastle" step that breaks that path fails HERE, with a behavioural symptom, not just a
 * classpath-identity flip in the probe.
 */
public class JwtBcPkixCharacterizationTest {

    @Test
    public void rsaSignedJwtRoundTripsThroughLiveBcpkix() throws Exception {
        // Evidence the path we exercise really is the live bcpkix jar (see "## bcpkix probe").
        System.out.println("[JWT-BCPKIX] PEMParser from: " + PEMParser.class.getProtectionDomain()
                .getCodeSource().getLocation());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // Render the private key to PEM with BouncyCastle's openssl writer (bcpkix).
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter w = new JcaPEMWriter(sw)) {
            w.writeObject(kp.getPrivate());
        }
        String privatePem = sw.toString();

        // RsaSigner(String) -> RsaKeyHelper.parseKeyPair -> org.bouncycastle.openssl.PEMParser.
        RsaSigner signer = new RsaSigner(privatePem);
        Jwt token = JwtHelper.encode("{\"sub\":\"alice\"}", signer);

        // Verify the signature round-trips, proving the bcpkix-parsed key actually signs.
        RsaVerifier verifier = new RsaVerifier((RSAPublicKey) kp.getPublic());
        Jwt decoded = JwtHelper.decodeAndVerify(token.getEncoded(), verifier);

        assertEquals("{\"sub\":\"alice\"}", decoded.getClaims());
    }
}