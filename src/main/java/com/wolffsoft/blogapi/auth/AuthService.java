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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_TOKEN_TYPE;
import static com.wolffsoft.blogapi.auth.TokenClaims.TYPE_REFRESH;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid token";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    @Transactional
    public void register(RegisterRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists!");
        }
        String hashed_password =  passwordEncoder.encode(request.password());
        User user = User.create(request.email(), request.name(), hashed_password);
        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String accessToken = tokenService.issueAccessToken(principal);
        String refreshToken = tokenService.issueRefreshToken(principal);
        return buildTokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(RefreshRequest refreshRequest) {
        Jwt jwt = decodeRefreshToken(refreshRequest.refreshToken());
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException(INVALID_TOKEN_MESSAGE));
        AppUserDetails userDetails = AppUserDetails.from(user);
        String accessToken = tokenService.issueAccessToken(userDetails);
        return buildTokenResponse(accessToken, refreshRequest.refreshToken());
    }

    private Jwt decodeRefreshToken(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException ex) {
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE);
        }
        if(!TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE);
        }
        return jwt;
    }

    private TokenResponse buildTokenResponse(String accessToken, String refreshToken) {
        return new TokenResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE_BEARER,
                jwtProperties.accessTokenTtl().toSeconds());
    }
}
