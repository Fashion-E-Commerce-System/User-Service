package com.ecommerce.backend.service;

import com.ecommerce.backend.domain.User;
import com.ecommerce.backend.dto.SignUpRequest;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisService redisService;

    @Transactional
    public TokenResponse login(SignUpRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        TokenResponse tokenResponse = jwtTokenProvider.createToken(authentication);

        redisService.setValues(
                authentication.getName(),
                tokenResponse.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenValidityInSeconds(),
                TimeUnit.SECONDS
        );

        return tokenResponse;
    }

    public void logout(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new IllegalArgumentException("Invalid token");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

        if (redisService.getValues(authentication.getName()) != null) {
            redisService.deleteValues(authentication.getName());
        }

        Long expiration = jwtTokenProvider.getExpiration(accessToken);
        redisService.setValues(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);

        String savedRefreshToken = redisService.getValues(authentication.getName());
        if (!refreshToken.equals(savedRefreshToken)) {
            throw new IllegalArgumentException("Mismatched refresh token");
        }

        TokenResponse tokenResponse = jwtTokenProvider.createToken(authentication);

        redisService.setValues(
                authentication.getName(),
                tokenResponse.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenValidityInSeconds(),
                TimeUnit.SECONDS
        );

        Long expiration = jwtTokenProvider.getExpiration(refreshToken);
        redisService.setValues(refreshToken, "logout", expiration, TimeUnit.MILLISECONDS);

        return tokenResponse;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getName())
                .password(user.getPassword())
                .roles(user.getRoles().toArray(new String[0]))
                .build();
    }
}