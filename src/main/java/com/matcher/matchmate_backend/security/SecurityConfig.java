package com.matcher.matchmate_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Stateless REST APIs using Bearer tokens do not need CSRF protection)
                .csrf(AbstractHttpConfigurer::disable)
                // 2. Enable CORS for Flutter app
                .cors(Customizer.withDefaults())
                // 3. Make session management STATELESS (no server-side HTTP sessions)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. NEW (C5): not logged in / bad token -> 401 Unauthorized (Spring's default would be 403)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // 5. Configure Endpoint Route Access
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()             // Unauthenticated health check
                        .requestMatchers("/api/v1/auth/**").permitAll()     // Public login handshake
                        .requestMatchers("/api/v1/branding/**").permitAll() // Public branding / config
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()                      // All other APIs require Bearer JWT
                )
                // 6. Add our JWT filter before the standard username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
