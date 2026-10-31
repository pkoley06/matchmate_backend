package com.matcher.matchmate_backend.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves photos written by {@link LocalDiskStorageService} — the dev-only stand-in for a
 * real bucket's public object URLs. Permitted without auth in SecurityConfig, same as a
 * real bucket object would be publicly readable.
 */
@RestController
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileController {

    private final LocalDiskStorageService storageService;

    public LocalFileController(LocalDiskStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/api/v1/uploads/files/**")
    public ResponseEntity<FileSystemResource> get(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = "/api/v1/uploads/files/";
        String key = path.substring(path.indexOf(prefix) + prefix.length());

        Path resolved = storageService.resolveForRead(key);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        MediaType contentType;
        try {
            String probed = Files.probeContentType(resolved);
            contentType = probed != null ? MediaType.parseMediaType(probed) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok().contentType(contentType).body(new FileSystemResource(resolved));
    }
}
