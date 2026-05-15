package com.neeraj.SpringEcom.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.neeraj.SpringEcom.Service.JwtService;
import com.neeraj.SpringEcom.Service.UserService;
import com.neeraj.SpringEcom.dto.UserResponse;
import com.neeraj.SpringEcom.exception.AuthException;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.dto.GoogleLoginRequest;
import com.neeraj.SpringEcom.model.dto.LoginRequest;
import com.neeraj.SpringEcom.model.dto.RegisterRequest;
import com.neeraj.SpringEcom.repo.UserRepo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final String googleClientId;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    public UserController(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepo userRepo
    ) {
        this.googleClientId = googleClientId;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPassword(request.password());
        user.setProvider("LOCAL");
        user.setRole("USER");

        User savedUser = userService.saveUser(user);

        UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getProvider(),
                savedUser.getRole()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        User existingUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!"LOCAL".equals(existingUser.getProvider())) {
            throw new AuthException("Please login with Google");
        }

        if (existingUser.getPassword() == null || existingUser.getPassword().isBlank()) {
            throw new AuthException("Please login with Google");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        if (!authentication.isAuthenticated()) {
            throw new AuthException("Invalid email or password");
        }

        String token = jwtService.generateToken(email);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", existingUser.getRole(),
                "username", existingUser.getUsername(),
                "email", existingUser.getEmail()
        ));
    }

    @PostMapping("/login/google")
    public ResponseEntity<Map<String, String>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
                .setAudience(List.of(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(request.idToken());

        if (idToken == null) {
            throw new AuthException("Invalid Google token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new AuthException("Google email is not verified");
        }

        String email = payload.getEmail().toLowerCase().trim();
        String username = (String) payload.get("name");

        if (username == null || username.isBlank()) {
            username = email.split("@")[0];
        }

        String finalUsername = username.trim();

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(finalUsername);
                    newUser.setProvider("GOOGLE");
                    newUser.setRole("USER");
                    newUser.setPassword(null);

                    return userRepo.save(newUser);
                });

        if (!"GOOGLE".equals(user.getProvider())) {
            throw new AuthException("Please login with email and password");
        }

        String token = jwtService.generateToken(email);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }
}
