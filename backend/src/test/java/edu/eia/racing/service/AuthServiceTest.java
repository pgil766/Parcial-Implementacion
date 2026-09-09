package edu.eia.racing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eia.racing.dto.AuthResponse;
import edu.eia.racing.dto.LoginRequest;
import edu.eia.racing.dto.RefreshRequest;
import edu.eia.racing.dto.RegisterRequest;
import edu.eia.racing.exception.DuplicateResourceException;
import edu.eia.racing.exception.InvalidCredentialsException;
import edu.eia.racing.model.Role;
import edu.eia.racing.model.User;
import edu.eia.racing.model.enums.RoleName;
import edu.eia.racing.repository.RoleRepository;
import edu.eia.racing.repository.UserRepository;
import edu.eia.racing.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Business rules of PROJECT_SPEC module 1, isolated from the database.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private static User userWith(RoleName roleName, String passwordHash, boolean enabled) {
        return User.builder()
                .id(1L)
                .username("racer")
                .email("racer@eia.edu.co")
                .passwordHash(passwordHash)
                .role(Role.builder().id(1L).name(roleName).build())
                .enabled(enabled)
                .build();
    }

    private void stubTokenGeneration() {
        when(jwtService.generateAccessToken(anyString(), any(RoleName.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString(), any(RoleName.class))).thenReturn("refresh-token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(3600L);
    }

    @Test
    void registerCreatesViewerAndHashesPassword() {
        when(userRepository.existsByUsername("racer")).thenReturn(false);
        when(userRepository.existsByEmail("racer@eia.edu.co")).thenReturn(false);
        when(roleRepository.findByName(RoleName.VIEWER))
                .thenReturn(Optional.of(Role.builder().id(3L).name(RoleName.VIEWER).build()));
        when(passwordEncoder.encode("supersecret")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTokenGeneration();

        AuthResponse response = authService.register(
                new RegisterRequest("racer", "racer@eia.edu.co", "supersecret"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        verify(auditLogService).record(any(User.class), eq("USER_REGISTERED"), eq("User"), isNull(), anyString());
        assertThat(savedUser.getValue().getRole().getName()).isEqualTo(RoleName.VIEWER);
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(response.role()).isEqualTo(RoleName.VIEWER);
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("racer")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("racer", "racer@eia.edu.co", "supersecret")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("racer")).thenReturn(false);
        when(userRepository.existsByEmail("racer@eia.edu.co")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("racer", "racer@eia.edu.co", "supersecret")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        when(userRepository.findByUsername("racer"))
                .thenReturn(Optional.of(userWith(RoleName.RACE_ORGANIZER, "hashed-password", true)));
        when(passwordEncoder.matches("supersecret", "hashed-password")).thenReturn(true);
        stubTokenGeneration();

        AuthResponse response = authService.login(new LoginRequest("racer", "supersecret"));

        verify(auditLogService).record(any(User.class), eq("LOGIN"), eq("User"), eq(1L), anyString());
        assertThat(response.role()).isEqualTo(RoleName.RACE_ORGANIZER);
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByUsername("racer"))
                .thenReturn(Optional.of(userWith(RoleName.VIEWER, "hashed-password", true)));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("racer", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void loginRejectsUnknownUserWithTheSameMessageAsAWrongPassword() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "supersecret")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void loginRejectsDisabledAccount() {
        when(userRepository.findByUsername("racer"))
                .thenReturn(Optional.of(userWith(RoleName.VIEWER, "hashed-password", false)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("racer", "supersecret")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void refreshRejectsInvalidToken() {
        when(jwtService.parseRefreshToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("invalid or expired");
    }
}
