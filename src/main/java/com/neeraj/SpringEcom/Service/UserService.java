package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.exception.EmailAlreadyRegisteredException;
import com.neeraj.SpringEcom.exception.InvalidUserException;
import com.neeraj.SpringEcom.model.AppConstants;
import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.repo.UserRepo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final BCryptPasswordEncoder encoder;
    private final UserRepo repo;

    public UserService(UserRepo repo, BCryptPasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
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
        user.setPassword(encoder.encode(user.getPassword()));

        return repo.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidUserException("Email is required");
        }

        String cleanEmail = email.toLowerCase().trim();

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