package com.bookforward.storage;

import com.bookforward.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bookforward.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageProvider implements StorageProvider {
    private final Path root;

    public LocalStorageProvider(AppProperties props) {
        this.root = Path.of(props.storage().localDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage directory " + root, e);
        }
    }

    @Override public String name() { return "local"; }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            Files.write(resolve(key), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Resource get(String key) { return new PathResource(resolve(key)); }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Illegal storage key");
        }
        return p;
    }
}
