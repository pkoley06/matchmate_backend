package com.matcher.matchmate_backend.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

/**
 * Stores uploaded photos in any S3-compatible bucket (real AWS S3, OCI Object Storage's
 * S3-compatibility API, MinIO, ...) selected via {@code app.storage.provider=s3}.
 * See deploy/DEPLOY-RUNBOOK.md Step F for how to set this up on OCI.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${app.storage.bucket}")
    private String bucket;

    @Value("${app.storage.region}")
    private String region;

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${app.storage.access-key}")
    private String accessKey;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @Value("${app.storage.path-style-access}")
    private boolean pathStyleAccess;

    @Value("${app.storage.public-base-url}")
    private String publicBaseUrl;

    private S3Client client;

    @PostConstruct
    void init() {
        requireNonBlank(bucket, "STORAGE_BUCKET");
        requireNonBlank(accessKey, "STORAGE_ACCESS_KEY");
        requireNonBlank(secretKey, "STORAGE_SECRET_KEY");
        requireNonBlank(publicBaseUrl, "STORAGE_PUBLIC_BASE_URL");

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(pathStyleAccess);

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        client = builder.build();
        log.info("S3StorageService initialized (bucket={}, endpoint={})", bucket, endpoint);
    }

    private static void requireNonBlank(String value, String envVarName) {
        if (value == null || value.isBlank()) {
            // Fail fast at startup (same pattern as DB_PASSWORD / JWT_SECRET) rather than
            // on the first photo upload a real user makes.
            throw new IllegalStateException(
                    "app.storage.provider=s3 but " + envVarName + " is not set. "
                            + "See deploy/DEPLOY-RUNBOOK.md Step F.");
        }
    }

    @Override
    public StoredObject upload(String key, byte[] data, String contentType) {
        client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));
        return new StoredObject(key, publicBaseUrl + "/" + key);
    }

    @Override
    public void delete(String url) {
        if (!isManagedUrl(url)) return;
        try {
            String key = url.substring((publicBaseUrl + "/").length());
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("Failed to delete stale storage object for url {}: {}", url, e.getMessage());
        }
    }

    @Override
    public boolean isManagedUrl(String url) {
        return url != null && url.startsWith(publicBaseUrl + "/");
    }
}
