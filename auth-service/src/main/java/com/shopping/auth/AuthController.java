package com.shopping.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class AuthController {

    private final Map<String, String> users = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "auth-service", "status", "UP");
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@RequestBody RegisterRequest request) {
        users.put(request.email(), request.password());
        return Map.of("message", "registered", "email", request.email());
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        String storedPassword = users.get(request.email());
        if (storedPassword == null || !storedPassword.equals(request.password())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String payload = request.email() + ":" + Instant.now().toEpochMilli();
        String token = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new LoginResponse("Bearer " + token);
    }

    public record RegisterRequest(@Email String email, @NotBlank String password) {
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token) {
    }
}
