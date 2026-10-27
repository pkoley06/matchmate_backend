package com.matcher.matchmate_backend.config;

import com.matcher.matchmate_backend.model.entity.AdminUser;
import com.matcher.matchmate_backend.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first admin console login on startup, from ADMIN_SEED_USERNAME /
 * ADMIN_SEED_PASSWORD (.env). Only runs while admin_users is empty, so it never resets
 * a password someone already changed. No plaintext default password is baked into the
 * code - set the env vars once, log in, and (eventually) add an admin password-change flow.
 */
@Component
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedUsername;
    private final String seedPassword;

    public AdminUserSeeder(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.core.env.Environment environment) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = environment.getProperty("ADMIN_SEED_USERNAME");
        this.seedPassword = environment.getProperty("ADMIN_SEED_PASSWORD");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }

        if (!StringUtils.hasText(seedUsername) || !StringUtils.hasText(seedPassword)) {
            log.warn("No admin console user exists yet and ADMIN_SEED_USERNAME/ADMIN_SEED_PASSWORD are not set "
                    + "(.env) - the admin console cannot be logged into until one is created.");
            return;
        }

        AdminUser admin = AdminUser.builder()
                .username(seedUsername)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .displayName("Admin")
                .build();
        adminUserRepository.save(admin);
        log.info("Seeded first admin console user '{}'.", seedUsername);
    }
}
