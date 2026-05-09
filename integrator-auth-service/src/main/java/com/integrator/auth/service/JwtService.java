package com.integrator.auth.service;

import com.integrator.auth.config.JwtProperties;
import com.integrator.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom =  new SecureRandom();

    public String generateAccessToken(User user) {
        return  Jwts.builder()
                .subject(user.getId().toString())
                .claim("username",user.getUsername())
                .claim("role",user.getRole())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(getRefreshTokenExpiry()))
                .signWith(rsaPrivateKey)
                .compact();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(rsaPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        Claims claims = validateAccessToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Instant getRefreshTokenExpiry() {
        return Instant.now().plus(jwtProperties.getAccessTokenExpiry());
    }

}
