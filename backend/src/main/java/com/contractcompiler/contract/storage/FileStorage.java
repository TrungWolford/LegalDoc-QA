package com.contractcompiler.contract.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for storing and retrieving uploaded contract files.
 * Implementations: {@link LocalFileStorage} for local disk, MinIO adapter for cloud.
 */
public interface FileStorage {

    /**
     * Stores the file content under a generated UUID-based name.
     *
     * @param inputStream file content
     * @param originalName original file name (used only to determine extension)
     * @return the storage path (relative to the storage root)
     */
    String store(InputStream inputStream, String originalName);

    /**
     * Retrieves the full filesystem path for a stored file.
     *
     * @param storagePath the path returned by {@link #store}
     * @return absolute path to the file
     */
    Path resolve(String storagePath);

    /**
     * Deletes a stored file.
     *
     * @param storagePath the path returned by {@link #store}
     */
    void delete(String storagePath);
}
