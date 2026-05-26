package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.exception.AuthException;
import com.neeraj.SpringEcom.exception.EmailAlreadyRegisteredException;
import com.neeraj.SpringEcom.exception.InvalidUserException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final BCryptPasswordEncoder encoder;
    private final UserRepo repo;
    private final EmailNormalizer emailNormalizer;

    public UserService(UserRepo repo, BCryptPasswordEncoder encoder) {
        this(repo, encoder, new EmailNormalizer());
    }

    @Autowired
    public UserService(
            UserRepo repo,
            BCryptPasswordEncoder encoder,
            EmailNormalizer emailNormalizer
    ) {
        this.repo = repo;
        this.encoder = encoder;
        this.emailNormalizer = emailNormalizer;
    }

    @Transactional
    public User saveUser(User user) {
        String email = normalizeEmail(user.getEmail());
        String username = normalizeUsername(user.getUsername());

        if (repo.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        validatePassword(user.getPassword());

        user.setEmail(email);
        user.setUsername(username);
        user.setProvider(AppConstants.Provider.LOCAL);
        user.setRole(AppConstants.Role.USER);
        user.setTokenVersion(0);
        user.setPassword(encoder.encode(user.getPassword()));

        return repo.save(user);
    }

    @Transactional
    public User findOrCreateGoogleUser(String email, String username) {
        String cleanEmail = normalizeEmail(email);
        String cleanUsername = normalizeUsername(username);

        return repo.findByEmail(cleanEmail)
                .orElseGet(() -> createGoogleUserAfterLookupMiss(cleanEmail, cleanUsername));
    }

    private User createGoogleUserAfterLookupMiss(String email, String username) {
        try {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(username);
            newUser.setProvider(AppConstants.Provider.GOOGLE);
            newUser.setRole(AppConstants.Role.USER);
            newUser.setPassword(null);
            newUser.setTokenVersion(0);

            return repo.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException e) {
            return repo.findByEmail(email)
                    .orElseThrow(() -> new AuthException("Unable to complete Google login"));
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidUserException("Email is required");
        }

        String cleanEmail = emailNormalizer.normalize(email);

        if (!cleanEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new InvalidUserException("Invalid email format");
        }

        return cleanEmail;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidUserException("Username is required");
        }

        return username.trim();
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidUserException("Password is required");
        }

        if (password.length() < 8) {
            throw new InvalidUserException("Password must be at least 8 characters");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidUserException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new InvalidUserException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new InvalidUserException("Password must contain at least one number");
        }
    }
}