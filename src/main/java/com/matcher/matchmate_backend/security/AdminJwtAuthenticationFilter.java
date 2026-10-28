package com.matcher.matchmate_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs only inside the admin SecurityFilterChain (see AdminSecurityConfig), which is
 * matched only for /api/v1/admin/**. Only accepts tokens minted by
 * JwtTokenProvider#generateAdminToken (aud=admin) - a mobile-user token is rejected here.
 */
@Component
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public AdminJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT validation for CORS preflight requests — the browser sends OPTIONS
        // without credentials. The actual authenticated request follows separately.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt) && jwtTokenProvider.isAdminToken(jwt)) {
            Long adminId = jwtTokenProvider.getAdminIdFromToken(jwt);
            String username = jwtTokenProvider.getUsernameFromToken(jwt);

            AdminAuthenticationPrincipal principal = new AdminAuthenticationPrincipal(adminId, username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
