package com.example.sobzybackend.config;

import com.example.sobzybackend.enums.Role;
import com.example.sobzybackend.repository.UserRepository;
import com.example.sobzybackend.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            log.info("Seeding default admin user...");
            User admin = User.builder()
                    .username("admin")
                    .email("admin@sobzy.com")
                    .password(passwordEncoder.encode("admin"))
                    .fullName("System Administrator")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .isLocked(false)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created successfully.");
        } else {
            log.debug("Admin user already exists. Skipping seeding.");
        }
    }
}
