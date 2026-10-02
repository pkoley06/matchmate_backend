package com.matcher.matchmate_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        // Already initialized (e.g. after a dev-tools restart or in tests)? Just reuse it.
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseAuth.getInstance();
        }

        if (credentialsPath.isBlank()) {
            throw new IllegalStateException(
                    "firebase.credentials-path is empty. Set FIREBASE_CREDENTIALS_PATH or fix application.yml.");
        }

        Resource resource = resourceLoader.getResource(credentialsPath);
        if (!resource.exists()) {
            // Fail fast (C7): a clear message now is better than a confusing crash later.
            throw new IllegalStateException(
                    "Firebase service account file not found at '" + credentialsPath + "'. "
                            + "Put it there (see Step 2.0) or set the FIREBASE_CREDENTIALS_PATH environment variable.");
        }

        try (InputStream is = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Initialized FirebaseApp with service account: {}", credentialsPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read Firebase service account file: " + credentialsPath, e);
        }

        return FirebaseAuth.getInstance();
    }
}