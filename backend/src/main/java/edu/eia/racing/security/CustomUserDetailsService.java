package edu.eia.racing.security;

import edu.eia.racing.model.User;
import edu.eia.racing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the {@code users} table to Spring Security. The transaction is required
 * because {@link User#getRole()} is a lazy association.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /** Spring Security expects role authorities to carry this prefix. */
    public static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().getName().name()))
                .disabled(!user.isEnabled())
                .build();
    }
}
