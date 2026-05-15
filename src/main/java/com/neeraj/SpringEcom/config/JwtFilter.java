
//package com.neeraj.SpringEcom.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.neeraj.SpringEcom.Service.JwtService;
//import com.neeraj.SpringEcom.Service.MyUserDetailsService;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.MediaType;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.time.Instant;
//import java.util.Map;
//
//@Component
//public class JwtFilter extends OncePerRequestFilter {
//
//    private final JwtService jwtService;
//    private final MyUserDetailsService userDetailsService;
//    private final ObjectMapper objectMapper;
//
//    public JwtFilter(
//            JwtService jwtService,
//            MyUserDetailsService userDetailsService,
//            ObjectMapper objectMapper
//    ) {
//        this.jwtService = jwtService;
//        this.userDetailsService = userDetailsService;
//        this.objectMapper = objectMapper;
//    }
//
//    @Override
//    protected void doFilterInternal(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            FilterChain filterChain
//    ) throws ServletException, IOException {
//
//        String header = request.getHeader("Authorization");
//
//        if (header == null || !header.startsWith("Bearer ")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String token = header.substring(7);
//        String username;
//
//        try {
//            username = jwtService.extractUserName(token);
//        } catch (Exception e) {
//            writeUnauthorizedResponse(response, "Invalid or expired token");
//            return;
//        }
//
//        if (username != null &&
//                SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//            if (jwtService.validateToken(token, userDetails)) {
//
//                UsernamePasswordAuthenticationToken auth =
//                        new UsernamePasswordAuthenticationToken(
//                                userDetails,
//                                null,
//                                userDetails.getAuthorities()
//                        );
//
//                auth.setDetails(
//                        new WebAuthenticationDetailsSource().buildDetails(request)
//                );
//
//                SecurityContextHolder.getContext().setAuthentication(auth);
//            } else {
//                writeUnauthorizedResponse(response, "Invalid or expired token");
//                return;
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private void writeUnauthorizedResponse(
//            HttpServletResponse response,
//            String message
//    ) throws IOException {
//
//        SecurityContextHolder.clearContext();
//
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//        objectMapper.writeValue(
//                response.getWriter(),
//                Map.of(
//                        "timestamp", Instant.now().toString(),
//                        "status", HttpServletResponse.SC_UNAUTHORIZED,
//                        "error", message
//                )
//        );
//    }
//}



package com.neeraj.SpringEcom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.Service.JwtService;
import com.neeraj.SpringEcom.Service.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtFilter(
            JwtService jwtService,
            MyUserDetailsService userDetailsService,
            ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String username = jwtService.extractUserName(token);

            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!jwtService.validateToken(token, userDetails)) {
                    writeUnauthorizedResponse(response, "Invalid or expired token");
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                auth.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception e) {
            writeUnauthorizedResponse(response, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getWriter(),
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpServletResponse.SC_UNAUTHORIZED,
                        "error", message
                )
        );
    }
}

