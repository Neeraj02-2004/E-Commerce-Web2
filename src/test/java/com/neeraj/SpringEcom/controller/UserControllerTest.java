package com.neeraj.SpringEcom.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.neeraj.SpringEcom.service.JwtService;
import com.neeraj.SpringEcom.service.MyUserDetailsService;
import com.neeraj.SpringEcom.service.UserService;
import com.neeraj.SpringEcom.exception.EmailAlreadyRegisteredException;
import com.neeraj.SpringEcom.exception.GlobalExceptionHandler;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-client-id"
})
class UserControllerTest {

    private static final String INVALID_LOGIN_MESSAGE = "Invalid email or password";
    private static final String INVALID_GOOGLE_TOKEN_MESSAGE = "Invalid Google token";
    private static final String REGISTRATION_RECEIVED_MESSAGE =
            "Registration request received. If this email can be registered, you may continue with login.";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MyUserDetailsService myUserDetailsService;

    @MockBean
    private UserRepo userRepo;

    @MockBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    void register_withValidRequest_shouldReturnAcceptedGenericMessage() throws Exception {
        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setUsername("Neeraj");
        savedUser.setEmail("buyer@example.com");
        savedUser.setProvider("LOCAL");
        savedUser.setRole("USER");
        savedUser.setTokenVersion(0);

        when(userService.saveUser(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Neeraj",
                                  "email": "BUYER@EXAMPLE.COM",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(REGISTRATION_RECEIVED_MESSAGE));
    }

    @Test
    void register_withDuplicateEmail_shouldReturnAcceptedGenericMessage() throws Exception {
        when(userService.saveUser(any(User.class)))
                .thenThrow(new EmailAlreadyRegisteredException("buyer@example.com"));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Neeraj",
                                  "email": "buyer@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(REGISTRATION_RECEIVED_MESSAGE));
    }

    @Test
    void register_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_withValidCredentials_shouldReturnTokenAndRole() throws Exception {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("Neeraj");
        existingUser.setEmail("buyer@example.com");
        existingUser.setPassword("encoded-password");
        existingUser.setProvider("LOCAL");
        existingUser.setRole("USER");
        existingUser.setTokenVersion(0);

        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.of(existingUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "buyer@example.com",
                        null,
                        List.of()
                ));
        when(jwtService.generateToken("buyer@example.com")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "BUYER@EXAMPLE.COM",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_withUnknownEmail_shouldReturnUniformUnauthorized() throws Exception {
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.message").value(INVALID_LOGIN_MESSAGE));
    }

    @Test
    void login_withWrongPassword_shouldReturnUniformUnauthorized() throws Exception {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("Neeraj");
        existingUser.setEmail("buyer@example.com");
        existingUser.setPassword("encoded-password");
        existingUser.setProvider("LOCAL");
        existingUser.setRole("USER");
        existingUser.setTokenVersion(0);

        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.of(existingUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "buyer@example.com",
                                  "password": "WrongPassword123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.message").value(INVALID_LOGIN_MESSAGE));
    }

    @Test
    void login_withGoogleAccount_shouldReturnUniformUnauthorized() throws Exception {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("Neeraj");
        existingUser.setEmail("buyer@example.com");
        existingUser.setPassword(null);
        existingUser.setProvider("GOOGLE");
        existingUser.setRole("USER");
        existingUser.setTokenVersion(0);

        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "buyer@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.message").value(INVALID_LOGIN_MESSAGE));
    }

    @Test
    void googleLogin_whenVerifierReturnsNull_shouldReturnUnauthorized() throws Exception {
        when(googleIdTokenVerifier.verify("bad-google-token")).thenReturn(null);

        mockMvc.perform(post("/api/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "bad-google-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.message").value(INVALID_GOOGLE_TOKEN_MESSAGE));
    }

    @Test
    void googleLogin_whenVerifierThrowsIOException_shouldReturnUnauthorized() throws Exception {
        when(googleIdTokenVerifier.verify("bad-google-token"))
                .thenThrow(new IOException("Google verification failed"));

        mockMvc.perform(post("/api/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "bad-google-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.message").value(INVALID_GOOGLE_TOKEN_MESSAGE));
    }

    @Test
    void logout_withBearerToken_shouldRevokeCurrentToken() throws Exception {
        mockMvc.perform(post("/api/logout")
                        .header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(jwtService).revokeToken("jwt-token");
    }

    @Test
    void logout_withoutBearerToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"));
    }

    @Test
    void logoutAll_withAuthenticatedPrincipal_shouldRevokeAllTokens() throws Exception {
        Principal principal = () -> "buyer@example.com";

        mockMvc.perform(post("/api/logout-all").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all devices successfully"));

        verify(jwtService).revokeAllTokens("buyer@example.com");
    }

    @Test
    void logoutAll_withoutPrincipal_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/logout-all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"));
    }
}