package com.integrator.route.authUtil;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

public class TestJwtFactory {
    private final NimbusJwtEncoder jwtEncoder;

    public TestJwtFactory(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
        RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .keyID("test-key")
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        this.jwtEncoder = new NimbusJwtEncoder((jwkSelector, context) ->
                jwkSelector.select(jwkSet)
        );
    }

    public String userToken() {
        return token("USER");
    }

    public String adminToken() {
        return token("ADMIN");
    }

    private String token(String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("username", role.toLowerCase() + "_test")
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
        JwsHeader header = JwsHeader.with(() -> "RS256")
                .keyId("test-key")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}