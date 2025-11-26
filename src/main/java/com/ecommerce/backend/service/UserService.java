package com.ecommerce.backend.service;

import com.ecommerce.backend.domain.User;
import com.ecommerce.backend.dto.SignUpRequest;
import com.ecommerce.backend.grpc.AuthGrpcClient;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthGrpcClient authGrpcClient;

    @Transactional
    public User createUser(SignUpRequest request) {
        if (userRepository.findByName(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        User user = User.builder()
                .name(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        // Call Auth service via gRPC
        try {
            authGrpcClient.callCreateUser(savedUser);
        } catch (Exception e) {
            // The gRPC client throws a RuntimeException, which will trigger a rollback
            log.error("gRPC call failed, transaction will be rolled back.", e);
            throw e; 
        }
        
        return savedUser;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        userRepository.delete(user);

        // Call Auth service via gRPC
        try {
            authGrpcClient.callDeleteUser(userId);
        } catch (Exception e) {
            // The gRPC client throws a RuntimeException, which will trigger a rollback
            log.error("gRPC call failed, transaction will be rolled back.", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getName())
                .password(user.getPassword())
                .authorities(java.util.Collections.emptyList()) // No roles in this service
                .build();
    }
}