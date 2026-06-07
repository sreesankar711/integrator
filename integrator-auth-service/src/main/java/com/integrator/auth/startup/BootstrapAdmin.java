package com.integrator.auth.startup;

import com.integrator.auth.config.BootstrapAdminProperties;
import com.integrator.auth.model.Role;
import com.integrator.auth.model.User;
import com.integrator.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdmin implements ApplicationRunner {
    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (!bootstrapAdminProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(bootstrapAdminProperties.getUsername()) || !StringUtils.hasText(bootstrapAdminProperties.getEmail()) || !StringUtils.hasText(bootstrapAdminProperties.getPassword())) {
            return;
        }
        if (userRepository.existsByUsername(bootstrapAdminProperties.getUsername())) {
            return;
        }
        User admin = User.builder()
                .username(bootstrapAdminProperties.getUsername())
                .email(bootstrapAdminProperties.getEmail())
                .password(passwordEncoder.encode(bootstrapAdminProperties.getPassword()))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
    }
}
