package com.contractcompiler.common.security;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.auth.AppUserRepository;
import com.contractcompiler.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter that extracts and validates Bearer tokens
 * from the Authorization header and sets the security context.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, AppUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            AppUser user = userRepository.findByEmail(email).orElse(null);

            if (user != null && user.isActive() && SecurityContextHolder.getContext().getAuthentication() == null) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException e) {
            // Invalid or expired token — continue without authentication
        }

        filterChain.doFilter(request, response);
    }
}
