package edu.eia.racing.service;

import edu.eia.racing.dto.AuthResponse;
import edu.eia.racing.dto.LoginRequest;
import edu.eia.racing.dto.RefreshRequest;
import edu.eia.racing.dto.RegisterRequest;
import edu.eia.racing.dto.UserProfileResponse;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidCredentialsException;
import edu.eia.racing.model.Role;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.RoleName;
import edu.eia.racing.repository.RoleRepository;
import edu.eia.racing.repository.UserRepository;
import edu.eia.racing.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication business rules (PROJECT_SPEC module 1).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Same text for "unknown user" and "wrong password" on purpose: a different
     * message per case would let an attacker enumerate valid usernames.
     */
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }

        // Self-registration always yields VIEWER. Letting the client choose the role
        // would allow anyone to grant themselves ADMIN; elevated accounts are created
        // by an administrator through the user management endpoints.
        Role viewerRole = roleRepository.findByName(RoleName.VIEWER)
                .orElseThrow(() -> new IllegalStateException("VIEWER role is missing from the database"));

        User user = userRepository.save(User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(viewerRole)
                .build());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("This account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String username = jwtService.parseRefreshToken(request.refreshToken())
                .map(Claims::getSubject)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is invalid or expired"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is invalid or expired"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("This account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user no longer exists"));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName(),
                user.isEnabled(),
                user.getCreatedAt());
    }

    private AuthResponse buildAuthResponse(User user) {
        RoleName role = user.getRole().getName();
        return new AuthResponse(
                jwtService.generateAccessToken(user.getUsername(), role),
                jwtService.generateRefreshToken(user.getUsername(), role),
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                user.getUsername(),
                role);
    }
}
