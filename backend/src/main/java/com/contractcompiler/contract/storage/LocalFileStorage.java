package com.contractcompiler.contract.storage;

import com.contractcompiler.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage for uploaded contract files.
 * Files are stored under random UUID names to prevent path traversal and name collisions.
 */
@Service
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path storageRoot;

    public LocalFileStorage(AppProperties appProperties) {
        this.storageRoot = Path.of(appProperties.upload().storageDir());
        try {
            Files.createDirectories(storageRoot);
            log.info("File storage initialized at: {}", storageRoot.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + storageRoot, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalName) {
        String extension = extractExtension(originalName);
        String storageName = UUID.randomUUID() + "." + extension;
        Path target = storageRoot.resolve(storageName);
        try {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} → {}", originalName, storageName);
            return storageName;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + originalName, e);
        }
    }

    @Override
    public Path resolve(String storagePath) {
        return storageRoot.resolve(storagePath);
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(storageRoot.resolve(storagePath));
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storagePath, e);
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "bin";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
