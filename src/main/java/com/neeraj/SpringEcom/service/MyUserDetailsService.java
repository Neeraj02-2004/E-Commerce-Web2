package com.neeraj.SpringEcom.service;

import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.UserPrincipal;
import com.neeraj.SpringEcom.repo.UserRepo;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepo repo;
    private final EmailNormalizer emailNormalizer;

    public MyUserDetailsService(UserRepo repo, EmailNormalizer emailNormalizer) {
        this.repo = repo;
        this.emailNormalizer = emailNormalizer;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }

        String normalizedEmail = emailNormalizer.normalize(email);

        User user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserPrincipal(user);
    }
}