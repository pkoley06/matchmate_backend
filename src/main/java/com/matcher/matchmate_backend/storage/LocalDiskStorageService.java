package com.matcher.matchmate_backend.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Writes uploaded photos to local disk and serves them back via {@link LocalFileController}.
 * This is the zero-setup default ({@code app.storage.provider=local}) so the upload feature
 * works immediately without any cloud account — but it is dev-only: the deployed container
 * runs with a read-only filesystem and no persistent volume, so this must never be used in
 * production. Switch to {@code app.storage.provider=s3} once a real bucket exists (see
 * deploy/DEPLOY-RUNBOOK.md Step F).
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalDiskStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskStorageService.class);

    @Value("${app.storage.local-dir}")
    private String localDir;

    @Value("${app.storage.public-base-url}")
    private String configuredPublicBaseUrl;

    @Value("${server.port}")
    private int serverPort;

    private Path root;
    private String publicBaseUrl;

    @PostConstruct
    void init() {
        root = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create local storage directory: " + root, e);
        }
        publicBaseUrl = (configuredPublicBaseUrl == null || configuredPublicBaseUrl.isBlank())
                ? "http://localhost:" + serverPort + "/api/v1/uploads/files"
                : configuredPublicBaseUrl;
        log.warn(
                "Using LOCAL disk storage at {} (dev-only — not safe in the prod container). "
                        + "Set STORAGE_PROVIDER=s3 once a bucket is set up.",
                root);
    }

    @Override
    public StoredObject upload(String key, byte[] data, String contentType) {
        Path target = resolveWithinRoot(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write local storage object: " + target, e);
        }
        return new StoredObject(key, publicBaseUrl + "/" + key);
    }

    @Override
    public void delete(String url) {
        if (!isManagedUrl(url)) return;
        String key = url.substring((publicBaseUrl + "/").length());
        try {
            Files.deleteIfExists(resolveWithinRoot(key));
        } catch (IOException e) {
            log.warn("Failed to delete stale local storage object for url {}: {}", url, e.getMessage());
        }
    }

    @Override
    public boolean isManagedUrl(String url) {
        return url != null && url.startsWith(publicBaseUrl + "/");
    }

    /** Streams the file for {@code key}, or null if it doesn't exist. Used by LocalFileController. */
    Path resolveForRead(String key) {
        Path resolved = resolveWithinRoot(key);
        return Files.exists(resolved) ? resolved : null;
    }

    private Path resolveWithinRoot(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return resolved;
    }
}
