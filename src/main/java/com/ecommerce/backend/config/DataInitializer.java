package com.ecommerce.backend.config;

import com.ecommerce.backend.domain.User;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.deleteAll();
        for (int i = 1; i <= 300; i++) {
            User user = new User();
            user.setName("testuser" + i);
            user.setPassword(passwordEncoder.encode("password"));
            user.setRoles(Collections.singletonList("USER"));
            userRepository.save(user);
        }
    }
}