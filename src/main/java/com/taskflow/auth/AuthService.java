package com.taskflow.auth;

import com.taskflow.security.JwtService;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication flows with refresh-token rotation: every use of a refresh
 * token revokes it and mints a fresh pair, so a stolen refresh token is only
 * useful until the legitimate client refreshes once (at which point the
 * legitimate refresh fails and the theft is detectable).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long refreshExpirationMs;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        if (users.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.MEMBER));
        users.save(user);
        return login(new LoginRequest(request.username(), request.password()));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserDetails user = (UserDetails) auth.getPrincipal();
        return issueTokens(user);
    }

    /**
     * Rotates the refresh token: the presented token is revoked and a new
     * pair is issued. Reusing an already-rotated token fails closed.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String rawToken = request.refreshToken();
        RefreshToken stored = lookupValidToken(rawToken);
        UserDetails user = loadActiveUser(stored.getUsername());
        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issueTokens(user);
    }

    /** Revokes a single refresh token (logout on one device). */
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokens.findByTokenHash(hash(request.refreshToken()))
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokens.save(t);
                });
    }

    /** Revokes every refresh token of the current user (logout everywhere). */
    @Transactional
    public void logoutAll(String username) {
        refreshTokens.findByUsernameAndRevokedFalse(username)
                .forEach(t -> t.setRevoked(true));
    }

    /** Hourly cleanup of expired/revoked tokens. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeTokens() {
        refreshTokens.deleteExpiredOrRevoked();
    }

    private RefreshToken lookupValidToken(String rawToken) {
        RefreshToken stored;
        try {
            if (!jwtService.isRefreshToken(rawToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a refresh token");
            }
            stored = refreshTokens.findByTokenHash(hash(rawToken))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Refresh token not recognized"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (stored.isRevoked() || stored.isExpired()
                || !jwtService.isValid(rawToken, loadActiveUser(stored.getUsername()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        return stored;
    }

    private AuthResponse issueTokens(UserDetails user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        RefreshToken stored = new RefreshToken();
        stored.setTokenHash(hash(refresh));
        stored.setUsername(user.getUsername());
        stored.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        refreshTokens.save(stored);
        Set<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return new AuthResponse(access, refresh, user.getUsername(), roles);
    }

    private UserDetails loadActiveUser(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is disabled");
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(),
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                        .toList());
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
