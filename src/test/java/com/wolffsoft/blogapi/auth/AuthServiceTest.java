package com.wolffsoft.blogapi.auth;

import com.wolffsoft.blogapi.auth.dto.LoginRequest;
import com.wolffsoft.blogapi.auth.dto.RefreshRequest;
import com.wolffsoft.blogapi.auth.dto.RegisterRequest;
import com.wolffsoft.blogapi.auth.dto.TokenResponse;
import com.wolffsoft.blogapi.auth.exception.EmailAlreadyExistsException;
import com.wolffsoft.blogapi.auth.exception.InvalidCredentialsException;
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException;
import com.wolffsoft.blogapi.user.User;
import com.wolffsoft.blogapi.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private User user;
    @Mock
    private Authentication authentication;
    @Mock
    private AppUserDetails principal;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register throws EmailAlreadyExistsException and never saves when the email is already taken")
    void registerExistingEmailThrowsEmailAlreadyExistsExceptionAndDoesNotSave() {
        // given
        RegisterRequest request = new RegisterRequest("existing@example.com", "Existing User", "plainTextPassword123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        // when / then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register saves the user with the password-encoder's output, not the raw password")
    void registerNewEmailSavesUserWithEncodedPasswordNotRawPassword() {
        // given
        RegisterRequest request = new RegisterRequest("new@example.com", "New User", "plainTextPassword123");
        String encodedHash = "bcrypt$encoded$hash";
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn(encodedHash);

        // when
        authService.register(request);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo(encodedHash);
        assertThat(savedUser.getPassword()).isNotEqualTo(request.password());
    }

    @Test
    @DisplayName("login throws a uniform InvalidCredentialsException when authentication fails, without leaking the underlying AuthenticationException")
    void loginAuthenticationFailsThrowsInvalidCredentialsExceptionWithUniformMessage() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "wrongPassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // when / then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("login returns a TokenResponse with the issued access/refresh tokens, Bearer type, and access-token TTL in seconds")
    void loginSuccessfulAuthenticationReturnsTokenResponse() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "correctPassword");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenService.issueAccessToken(principal)).thenReturn("access-token-value");
        when(tokenService.issueRefreshToken(principal)).thenReturn("refresh-token-value");
        when(jwtProperties.accessTokenTtl()).thenReturn(Duration.ofSeconds(900));

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token-value");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("refresh returns a new access token while echoing back the same refresh token when the refresh token is valid")
    void refreshValidRefreshTokenReturnsNewAccessTokenAndSameRefreshToken() {
        // given
        UUID userId = UUID.randomUUID();
        String refreshTokenValue = "valid-refresh-token";
        RefreshRequest request = new RefreshRequest(refreshTokenValue);
        Jwt decodedJwt = Jwt.withTokenValue(refreshTokenValue)
                .header("alg", "HS256")
                .claim("token_type", "refresh")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(jwtDecoder.decode(refreshTokenValue)).thenReturn(decodedJwt);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenService.issueAccessToken(any(AppUserDetails.class))).thenReturn("new-access-token");
        when(jwtProperties.accessTokenTtl()).thenReturn(Duration.ofSeconds(900));

        // when
        TokenResponse response = authService.refresh(request);

        // then
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
    }

    @Test
    @DisplayName("refresh throws InvalidTokenException when the decoded token's token_type is not \"refresh\" (e.g. an access token)")
    void refreshTokenTypeIsNotRefreshThrowsInvalidTokenException() {
        // given
        String accessTokenValue = "access-token-used-as-refresh";
        RefreshRequest request = new RefreshRequest(accessTokenValue);
        Jwt decodedJwt = Jwt.withTokenValue(accessTokenValue)
                .header("alg", "HS256")
                .claim("token_type", "access")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(jwtDecoder.decode(accessTokenValue)).thenReturn(decodedJwt);

        // when / then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("refresh throws InvalidTokenException when the token fails to decode (malformed/expired)")
    void refreshTokenFailsToDecodeThrowsInvalidTokenException() {
        // given
        String malformedToken = "not.a.valid.token";
        RefreshRequest request = new RefreshRequest(malformedToken);
        when(jwtDecoder.decode(malformedToken)).thenThrow(new JwtException("malformed token"));

        // when / then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("refresh throws InvalidTokenException when the valid token's subject does not match any existing user")
    void refreshSubjectUserNotFoundThrowsInvalidTokenException() {
        // given
        UUID userId = UUID.randomUUID();
        String refreshTokenValue = "valid-token-unknown-user";
        RefreshRequest request = new RefreshRequest(refreshTokenValue);
        Jwt decodedJwt = Jwt.withTokenValue(refreshTokenValue)
                .header("alg", "HS256")
                .claim("token_type", "refresh")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(jwtDecoder.decode(refreshTokenValue)).thenReturn(decodedJwt);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);
    }
}