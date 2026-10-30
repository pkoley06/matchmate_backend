package com.matcher.matchmate_backend.security;

import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AppUserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = extractJwtFromRequest(request);

        if( StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt) && !jwtTokenProvider.isAdminToken(jwt) ){
            Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
            String firebaseUid = jwtTokenProvider.getFirebaseUidFromToken(jwt);

            // Check if user is suspended or deactivated
            Optional<AppUser> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                if (Boolean.TRUE.equals(user.getIsSuspended())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    String reason = user.getSuspendedReason() != null ? user.getSuspendedReason() : "Your account has been suspended.";
                    response.getWriter().write("{\"error\":\"ACCOUNT_SUSPENDED\",\"message\":\"" + reason + "\"}");
                    return;
                }
                if (!Boolean.TRUE.equals(user.getIsActive())) {
                    // Stateless JWTs: the active flag is the revocation list. A disabled (paused)
                    // account gets a distinct code so the client can explain it was the user's choice.
                    boolean disabled = user.getAccountStatus() == com.matcher.matchmate_backend.model.enums.auth.AccountStatus.DISABLED;
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(disabled
                            ? "{\"error\":\"ACCOUNT_DISABLED\",\"message\":\"Your account is disabled. Sign in again to re-activate it.\"}"
                            : "{\"error\":\"ACCOUNT_DEACTIVATED\",\"message\":\"Your account has been deactivated.\"}");
                    return;
                }
            }

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, firebaseUid);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set user in Spring Security Context for this request thread
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
