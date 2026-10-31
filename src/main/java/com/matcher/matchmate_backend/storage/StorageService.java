package com.matcher.matchmate_backend.storage;

/**
 * Object storage abstraction for uploaded photos. Swappable via {@code app.storage.provider}
 * (see application.yml) between a local-disk dev fallback and any S3-compatible bucket
 * (AWS S3, OCI Object Storage, MinIO, ...) with zero code changes.
 */
public interface StorageService {

    /** Uploads {@code data} under {@code key} and returns its publicly readable URL. */
    StoredObject upload(String key, byte[] data, String contentType);

    /**
     * Best-effort delete of a previously uploaded object, identified by the URL this service
     * returned for it. No-ops (rather than throwing) if the URL isn't one of ours or the
     * object is already gone — callers use this for background cleanup, never as something
     * a request should fail over.
     */
    void delete(String url);

    /** True if {@code url} was produced by this storage service (so it's safe to {@link #delete}). */
    boolean isManagedUrl(String url);

    record StoredObject(String key, String url) {}
}
