package com.neeraj.SpringEcom.Controller;

import com.neeraj.SpringEcom.Service.JwtService;
import com.neeraj.SpringEcom.Service.MyUserDetailsService;
import com.neeraj.SpringEcom.Service.UserService;
import com.neeraj.SpringEcom.controller.UserController;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void register_withValidRequest_shouldCreateUser() throws Exception {
        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setUsername("Neeraj");
        savedUser.setEmail("buyer@example.com");
        savedUser.setProvider("LOCAL");
        savedUser.setRole("USER");

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("Neeraj"))
                .andExpect(jsonPath("$.email").value("buyer@example.com"))
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_withDuplicateEmail_shouldReturnConflict() throws Exception {
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
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
    void login_withUnknownEmail_shouldReturnUnauthorized() throws Exception {
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
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"));
    }

    @Test
    void login_withWrongPassword_shouldReturnUnauthorized() throws Exception {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("Neeraj");
        existingUser.setEmail("buyer@example.com");
        existingUser.setPassword("encoded-password");
        existingUser.setProvider("LOCAL");
        existingUser.setRole("USER");

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
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withGoogleAccount_shouldReturnUnauthorized() throws Exception {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("Neeraj");
        existingUser.setEmail("buyer@example.com");
        existingUser.setPassword(null);
        existingUser.setProvider("GOOGLE");
        existingUser.setRole("USER");

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
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"));
    }
}
