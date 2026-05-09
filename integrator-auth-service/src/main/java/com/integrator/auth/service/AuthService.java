package com.integrator.auth.service;

import com.integrator.auth.dto.*;
import com.integrator.auth.model.RefreshToken;
import com.integrator.auth.model.User;
import com.integrator.auth.repository.RefreshTokenRepository;
import com.integrator.auth.repository.UserRepository;
import com.integrator.auth.util.TokenHashUtils;
import jakarta.transaction.Transactional;
import com.integrator.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserDto register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new ValidationException("Username is already in use");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ValidationException("Email is already in use");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        User savedUser = userRepository.save(user);

        return UserDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    public TokenPair  login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ValidationException("Invalid credentials"));

        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if(!passwordMatches) {
            throw new ValidationException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();
        String refreshTokenHash = TokenHashUtils.hashedToken(refreshToken);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(refreshTokenHash)
                        .expiresAt(jwtService.getRefreshTokenExpiry())
                        .build()
        );

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    public TokenPair refresh(String refreshToken) {
        String refreshTokenHash =  TokenHashUtils.hashedToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> new ValidationException("Invalid refresh token"));
        if(storedToken.isRevoked()) {
            throw new ValidationException("Refresh token already revoked");
        }
        if(storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Refresh Token expired");
        }
        storedToken.setRevoked(true);

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken();
        String newRefreshTokenHash = TokenHashUtils.hashedToken(newRefreshToken);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(newRefreshTokenHash)
                        .expiresAt(jwtService.getRefreshTokenExpiry())
                        .build()
        );

        return TokenPair.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String refreshToken) {
        String  refreshTokenHash = TokenHashUtils.hashedToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> new ValidationException("Invalid refresh token"));
        storedToken.setRevoked(true);
    }

}
