package com.wolffsoft.blogapi.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.wolffsoft.blogapi.auth.Roles.ROLE_ADMIN;
import static com.wolffsoft.blogapi.auth.Roles.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserProviderTest {

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUserId returns the UUID parsed from the authenticated JWT's subject claim")
    void getCurrentUserIdAuthenticatedJwtReturnsSubjectAsUuid() {
        // given
        UUID userId = UUID.randomUUID();
        JwtAuthenticationToken token = jwtAuthenticationToken(userId, List.of(new SimpleGrantedAuthority(ROLE_USER)));
        SecurityContextHolder.getContext().setAuthentication(token);

        // when
        UUID result = currentUserProvider.getCurrentUserId();

        // then
        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("isCurrentUserAdmin returns true when the authenticated JWT's authorities contain ROLE_ADMIN")
    void isCurrentUserAdminAuthoritiesContainRoleAdminReturnsTrue() {
        // given
        JwtAuthenticationToken token =
                jwtAuthenticationToken(UUID.randomUUID(), List.of(new SimpleGrantedAuthority(ROLE_ADMIN)));
        SecurityContextHolder.getContext().setAuthentication(token);

        // when
        boolean result = currentUserProvider.isCurrentUserAdmin();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isCurrentUserAdmin returns false when the authenticated JWT's authorities do not contain ROLE_ADMIN")
    void isCurrentUserAdminAuthoritiesDoNotContainRoleAdminReturnsFalse() {
        // given
        JwtAuthenticationToken token =
                jwtAuthenticationToken(UUID.randomUUID(), List.of(new SimpleGrantedAuthority(ROLE_USER)));
        SecurityContextHolder.getContext().setAuthentication(token);

        // when
        boolean result = currentUserProvider.isCurrentUserAdmin();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getCurrentUserId throws IllegalStateException when the security context holds no authentication")
    void getCurrentUserIdNoAuthenticationThrowsIllegalStateException() {
        // given
        SecurityContextHolder.getContext().setAuthentication(null);

        // when / then
        assertThatThrownBy(currentUserProvider::getCurrentUserId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getCurrentUserId throws IllegalStateException when the authentication is not a JWT principal")
    void getCurrentUserIdNonJwtAuthenticationThrowsIllegalStateException() {
        // given
        UsernamePasswordAuthenticationToken nonJwtAuthentication =
                new UsernamePasswordAuthenticationToken("user@example.com", "password");
        SecurityContextHolder.getContext().setAuthentication(nonJwtAuthentication);

        // when / then
        assertThatThrownBy(currentUserProvider::getCurrentUserId)
                .isInstanceOf(IllegalStateException.class);
    }

    private JwtAuthenticationToken jwtAuthenticationToken(UUID subject, List<GrantedAuthority> authorities) {
        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .subject(subject.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        return new JwtAuthenticationToken(jwt, authorities);
    }
}