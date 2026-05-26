package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.AuthException;
import com.neeraj.SpringEcom.exception.EmailAlreadyRegisteredException;
import com.neeraj.SpringEcom.exception.InvalidUserException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void saveUser_withValidUser_shouldNormalizeEncodeAndSave() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        User user = new User();
        user.setUsername(" Neeraj ");
        user.setEmail(" BUYER@EXAMPLE.COM ");
        user.setPassword("Password123");

        when(userRepo.existsByEmail("buyer@example.com")).thenReturn(false);
        when(encoder.encode("Password123")).thenReturn("encoded-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userRepo, encoder);

        User savedUser = service.saveUser(user);

        assertThat(savedUser.getUsername()).isEqualTo("Neeraj");
        assertThat(savedUser.getEmail()).isEqualTo("buyer@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getProvider()).isEqualTo(AppConstants.Provider.LOCAL);
        assertThat(savedUser.getRole()).isEqualTo(AppConstants.Role.USER);
        assertThat(savedUser.getTokenVersion()).isZero();

        verify(userRepo).save(user);
    }

    @Test
    void saveUser_withDuplicateEmail_shouldThrow() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        User user = new User();
        user.setUsername("Neeraj");
        user.setEmail("buyer@example.com");
        user.setPassword("Password123");

        when(userRepo.existsByEmail("buyer@example.com")).thenReturn(true);

        UserService service = new UserService(userRepo, encoder);

        assertThatThrownBy(() -> service.saveUser(user))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void saveUser_withWeakPassword_shouldThrow() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        User user = new User();
        user.setUsername("Neeraj");
        user.setEmail("buyer@example.com");
        user.setPassword("short");

        when(userRepo.existsByEmail("buyer@example.com")).thenReturn(false);

        UserService service = new UserService(userRepo, encoder);

        assertThatThrownBy(() -> service.saveUser(user))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void findOrCreateGoogleUser_whenUserExists_shouldReturnExistingUser() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        User existingUser = googleUser("buyer@example.com");

        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.of(existingUser));

        UserService service = new UserService(userRepo, encoder);

        User result = service.findOrCreateGoogleUser(" BUYER@EXAMPLE.COM ", " Neeraj ");

        assertThat(result).isSameAs(existingUser);
        verify(userRepo, never()).saveAndFlush(any(User.class));
    }

    @Test
    void findOrCreateGoogleUser_whenUserMissing_shouldCreateGoogleUser() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.empty());
        when(userRepo.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userRepo, encoder);

        User result = service.findOrCreateGoogleUser(" BUYER@EXAMPLE.COM ", " Neeraj ");

        assertThat(result.getEmail()).isEqualTo("buyer@example.com");
        assertThat(result.getUsername()).isEqualTo("Neeraj");
        assertThat(result.getProvider()).isEqualTo(AppConstants.Provider.GOOGLE);
        assertThat(result.getRole()).isEqualTo(AppConstants.Role.USER);
        assertThat(result.getPassword()).isNull();
        assertThat(result.getTokenVersion()).isZero();

        verify(userRepo).saveAndFlush(any(User.class));
    }

    @Test
    void findOrCreateGoogleUser_whenConcurrentCreateWinsElsewhere_shouldRefetchAndReturnExistingUser() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        User existingUser = googleUser("buyer@example.com");

        when(userRepo.findByEmail("buyer@example.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingUser));
        when(userRepo.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        UserService service = new UserService(userRepo, encoder);

        User result = service.findOrCreateGoogleUser("buyer@example.com", "Neeraj");

        assertThat(result).isSameAs(existingUser);
        verify(userRepo).saveAndFlush(any(User.class));
    }

    @Test
    void findOrCreateGoogleUser_whenConflictButUserStillMissing_shouldThrowAuthException() {
        UserRepo userRepo = mock(UserRepo.class);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

        when(userRepo.findByEmail("buyer@example.com")).thenReturn(Optional.empty());
        when(userRepo.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        UserService service = new UserService(userRepo, encoder);

        assertThatThrownBy(() -> service.findOrCreateGoogleUser("buyer@example.com", "Neeraj"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Unable to complete Google login");
    }

    private User googleUser(String email) {
        User user = new User();
        user.setId(1);
        user.setUsername("Neeraj");
        user.setEmail(email);
        user.setPassword(null);
        user.setProvider(AppConstants.Provider.GOOGLE);
        user.setRole(AppConstants.Role.USER);
        user.setTokenVersion(0);
        return user;
    }
}