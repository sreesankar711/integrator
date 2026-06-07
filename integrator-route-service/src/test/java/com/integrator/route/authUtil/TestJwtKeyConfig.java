package com.integrator.route.authUtil;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@TestConfiguration
public class TestJwtKeyConfig {

    @Bean
    public RSAPrivateKey rsaPrivateKey(Environment environment) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String key = new String(new DefaultResourceLoader().getResource(environment.getProperty("JWT_PRIVATE_KEY_PATH", "")).getInputStream().readAllBytes());
        key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    @Bean
    public TestJwtFactory testJwtFactory(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
        return new TestJwtFactory(rsaPublicKey, rsaPrivateKey);
    }
}
