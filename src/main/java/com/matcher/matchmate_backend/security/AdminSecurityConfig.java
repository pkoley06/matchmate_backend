package com.matcher.matchmate_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CI CD
 * Separate filter chain for the Flutter-web admin console, matched only for /api/v1/admin/**.
 * Must be @Order(1) (checked before SecurityConfig's catch-all chain, which is @Order(2)).
 */
@Configuration
public class AdminSecurityConfig {

    private final AdminJwtAuthenticationFilter adminJwtAuthenticationFilter;

    public AdminSecurityConfig(AdminJwtAuthenticationFilter adminJwtAuthenticationFilter) {
        this.adminJwtAuthenticationFilter = adminJwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Global CORS configuration source used by both security filter chains via
     * {@code .cors(Customizer.withDefaults())}. Allows the Flutter-web admin app
     * (typically served on localhost:PORT during dev) to make cross-origin JSON requests
     * including the Authorization header.
     *
     * In production, replace the wildcard origin with the actual admin-panel domain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow any origin in dev; tighten this to the deployed admin URL in production.
        config.setAllowedOriginPatterns(List.of("*"));

        // All standard HTTP verbs + the OPTIONS preflight verb.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow Authorization header so the JWT Bearer token can be sent.
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization back to the client if needed.
        config.setExposedHeaders(List.of("Authorization"));

        // Required when credentials (cookies / Authorization header) are sent.
        config.setAllowCredentials(true);

        // How long the browser may cache the preflight response (10 minutes).
        config.setMaxAge(600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/auth/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterBefore(adminJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
