package com.wolffsoft.blogapi.auth;

import com.wolffsoft.blogapi.user.User;
import com.wolffsoft.blogapi.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername returns UserDetails with username/password from the found user and ROLE_USER for a non-admin user")
    void loadUserByUsernameNonAdminUserReturnsUserDetailsWithUserRole() {
        // given
        String email = "user@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn(email);
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getIsAdmin()).thenReturn(false);

        // when
        UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);

        // then
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername returns UserDetails with ROLE_ADMIN for an admin user")
    void loadUserByUsernameAdminUserReturnsUserDetailsWithAdminRole() {
        // given
        String email = "admin@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn(email);
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getIsAdmin()).thenReturn(true);

        // when
        UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);

        // then
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when no user matches the given email")
    void loadUserByUsernameUnknownEmailThrowsUsernameNotFoundException() {
        // given
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> appUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}