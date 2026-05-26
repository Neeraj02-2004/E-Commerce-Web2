package com.neeraj.SpringEcom.security;

import com.neeraj.SpringEcom.exception.UserNotAuthenticatedException;
import com.neeraj.SpringEcom.util.EmailNormalizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final EmailNormalizer emailNormalizer;

    public CurrentUserProvider(EmailNormalizer emailNormalizer) {
        this.emailNormalizer = emailNormalizer;
    }

    public String getAuthenticatedEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        return emailNormalizer.normalize(auth.getName());
    }
}