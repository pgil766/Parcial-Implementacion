package edu.eia.racing.controller;

import edu.eia.racing.dto.AuthResponse;
import edu.eia.racing.dto.LoginRequest;
import edu.eia.racing.dto.RefreshRequest;
import edu.eia.racing.dto.RegisterRequest;
import edu.eia.racing.dto.UserProfileResponse;
import edu.eia.racing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints from PROJECT_SPEC module 1.
 * Holds no business logic: it validates the payload shape and delegates.
 *
 * <p>There is no server-side logout endpoint by design: with stateless JWTs the
 * client logs out by discarding its tokens.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> profile(Authentication authentication) {
        return ResponseEntity.ok(authService.profile(authentication.getName()));
    }
}
