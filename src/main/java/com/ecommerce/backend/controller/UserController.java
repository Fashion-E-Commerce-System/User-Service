package com.ecommerce.backend.controller;

import com.ecommerce.backend.domain.User;
import com.ecommerce.backend.dto.SignUpRequest;
import com.ecommerce.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> signUp(@RequestBody SignUpRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.created(URI.create("/users/" + user.getUsername())).body(user);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserDetails> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}