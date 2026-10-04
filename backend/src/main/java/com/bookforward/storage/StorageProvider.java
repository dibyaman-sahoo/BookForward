package com.bookforward.storage;

import org.springframework.core.io.Resource;

/** Provider-neutral adapter boundary (local disk now; S3/GCS/Azure adapters can be added later). */
public interface StorageProvider {
    String name();
    void put(String key, byte[] content, String contentType);
    Resource get(String key);
    void delete(String key);
}
