package com.wolffsoft.blogapi.auth;

import com.wolffsoft.blogapi.user.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.wolffsoft.blogapi.auth.Roles.ROLE_ADMIN;
import static com.wolffsoft.blogapi.auth.Roles.ROLE_USER;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AppUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final boolean isAdmin;

    @Override
    public Collection<? extends GrantedAuthority>  getAuthorities() {
        return List.of(new SimpleGrantedAuthority(isAdmin ? ROLE_ADMIN : ROLE_USER));
    }

    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled()               { return true; }
    public UUID getId() { return id; }

    public static AppUserDetails from(User user) {
        return new AppUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getIsAdmin()
        );
    }
}
