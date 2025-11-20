package com.bmcho.userservice.controller;

import com.bmcho.userservice.dto.UserDto;
import com.bmcho.userservice.entity.User;
import com.bmcho.userservice.service.JWTService;
import com.bmcho.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class AuthController {

    private final JWTService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserDto.LoginRequest request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(UserDto.LoginResponse.builder()
                .token(token)
                .user(UserDto.Response.from(user))
                .build());
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(
            @RequestBody UserDto.TokenRequest request) {
        Claims claims = jwtService.validateToken(request.getToken());
        return ResponseEntity.ok(UserDto.TokenResponse.builder()
                .email(claims.getSubject())
                .valid(true)
                .role(claims.get("role", String.class))
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestBody UserDto.TokenRequest tokenRequest) {
        String newToken = jwtService.refreshToken(tokenRequest.getToken());
        return ResponseEntity.ok(Collections.singletonMap("token", newToken));
    }
}