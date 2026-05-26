package com.neeraj.SpringEcom.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.UserService;
import com.neeraj.SpringEcom.exception.AuthException;
import com.neeraj.SpringEcom.exception.EmailAlreadyRegisteredException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.dto.GoogleLoginRequest;
import com.neeraj.SpringEcom.model.dto.LoginRequest;
import com.neeraj.SpringEcom.model.dto.RegisterRequest;
import com.neeraj.SpringEcom.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final String INVALID_LOGIN_MESSAGE = "Invalid email or password";
    private static final String INVALID_GOOGLE_TOKEN_MESSAGE = "Invalid Google token";
    private static final String REGISTRATION_RECEIVED_MESSAGE =
            "Registration request received. If this email can be registered, you may continue with login.";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepo userRepo;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public UserController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepo userRepo,
            GoogleIdTokenVerifier googleIdTokenVerifier
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPassword(request.password());
        user.setProvider(AppConstants.Provider.LOCAL);
        user.setRole(AppConstants.Role.USER);
        user.setTokenVersion(0);

        try {
            userService.saveUser(user);
        } catch (EmailAlreadyRegisteredException ignored) {
            // Intentionally return the same response to avoid email enumeration.
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", REGISTRATION_RECEIVED_MESSAGE
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        try {
            User existingUser = userRepo.findByEmail(email)
                    .orElseThrow(() -> new AuthException(INVALID_LOGIN_MESSAGE));

            if (!AppConstants.Provider.LOCAL.equals(existingUser.getProvider())) {
                throw new AuthException(INVALID_LOGIN_MESSAGE);
            }

            if (existingUser.getPassword() == null || existingUser.getPassword().isBlank()) {
                throw new AuthException(INVALID_LOGIN_MESSAGE);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );

            if (!authentication.isAuthenticated()) {
                throw new AuthException(INVALID_LOGIN_MESSAGE);
            }

            String token = jwtService.generateToken(email);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", existingUser.getRole(),
                    "username", existingUser.getUsername(),
                    "email", existingUser.getEmail()
            ));
        } catch (AuthenticationServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(INVALID_LOGIN_MESSAGE);
        }
    }

    @PostMapping("/login/google")
    public ResponseEntity<Map<String, String>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        GoogleIdToken idToken = verifyGoogleToken(request.idToken());

        GoogleIdToken.Payload payload = idToken.getPayload();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new AuthException(INVALID_GOOGLE_TOKEN_MESSAGE);
        }

        String email = payload.getEmail().toLowerCase().trim();
        String username = (String) payload.get("name");

        if (username == null || username.isBlank()) {
            username = email.split("@")[0];
        }

        User user = userService.findOrCreateGoogleUser(email, username.trim());

        if (!AppConstants.Provider.GOOGLE.equals(user.getProvider())) {
            throw new AuthException(INVALID_GOOGLE_TOKEN_MESSAGE);
        }

        String token = jwtService.generateToken(email);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        jwtService.revokeToken(token);

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully"
        ));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AuthException("User not authenticated");
        }

        jwtService.revokeAllTokens(principal.getName());

        return ResponseEntity.ok(Map.of(
                "message", "Logged out from all devices successfully"
        ));
    }

    private GoogleIdToken verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken verifiedToken = googleIdTokenVerifier.verify(idToken);

            if (verifiedToken == null) {
                throw new AuthException(INVALID_GOOGLE_TOKEN_MESSAGE);
            }

            return verifiedToken;
        } catch (GeneralSecurityException | IOException e) {
            throw new AuthException(INVALID_GOOGLE_TOKEN_MESSAGE);
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthException("Bearer token is required");
        }

        return header.substring(7);
    }
}