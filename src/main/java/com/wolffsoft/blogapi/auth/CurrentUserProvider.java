package com.wolffsoft.blogapi.auth;

import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.wolffsoft.blogapi.auth.Roles.ROLE_ADMIN;

@Component
@NoArgsConstructor
public class CurrentUserProvider {

    public UUID getCurrentUserId() {
        JwtAuthenticationToken token =  requireJwtAuthentication();
        String subject = getSubject(token);
        return UUID.fromString(subject);
    }

    public boolean isCurrentUserAdmin() {
        JwtAuthenticationToken token =  requireJwtAuthentication();
        return token.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }

    private String getSubject(JwtAuthenticationToken token) {
        return token.getToken().getSubject();
    }

    private JwtAuthenticationToken requireJwtAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken;
        }
        throw new IllegalStateException("No authenticated JWT principal present");
    }
}
