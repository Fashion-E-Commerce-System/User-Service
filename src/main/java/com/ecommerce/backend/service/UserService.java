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
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);


        try {
            authGrpcClient.callCreateUser(savedUser);
        } catch (Exception e) {
            log.error("gRPC call failed, transaction will be rolled back.", e);
            throw e; 
        }
        
        return savedUser;
    }

    @Transactional
    public void deleteUser(String username) {
        log.info("Deleting user {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + username));

        userRepository.delete(user);


        try {
            authGrpcClient.callDeleteUser(username);
        } catch (Exception e) {

            log.error("gRPC call failed, transaction will be rolled back.", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(java.util.Collections.emptyList())
                .build();
    }
}