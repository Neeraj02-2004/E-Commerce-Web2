package com.neeraj.SpringEcom.Service;

import com.neeraj.SpringEcom.model.User;
import com.neeraj.SpringEcom.model.UserPrincipal;
import com.neeraj.SpringEcom.repo.UserRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepo repo;

    public MyUserDetailsService(UserRepo repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }

        String normalizedEmail = email.toLowerCase().trim();

        User user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));

        return new UserPrincipal(user);
    }
}
