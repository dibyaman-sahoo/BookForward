package com.bookforward.storage;

import com.bookforward.config.AppProperties;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.repository.StorageObjectRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Validates uploads, generates safe keys and records metadata; the bytes live in a StorageProvider. */
@Service
@RequiredArgsConstructor
public class StorageService {
    private static final Pattern SAFE_KEY = Pattern.compile("^[a-f0-9-]{36}\\.(jpg|png|webp)$");
    private static final Set<String> EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final StorageProvider provider;
    private final StorageObjectRepository objects;
    private final AppProperties props;

    @Transactional
    public StorageObject store(User owner, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "No file was uploaded");
        }
        if (file.getSize() > props.storage().maxBytes()) {
            throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Image must be 5 MB or smaller");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("UNREADABLE_FILE", "The file could not be read");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        String detected = detectType(bytes);
        if (!EXT.contains(ext) || detected == null || !matches(ext, detected)
                || (file.getContentType() != null && !file.getContentType().equalsIgnoreCase(contentType(detected)))) {
            throw ApiException.unprocessable("INVALID_IMAGE", "Only genuine JPEG, PNG or WebP images are accepted");
        }
        String key = UUID.randomUUID() + "." + detected;
        provider.put(key, bytes, contentType(detected));

        StorageObject o = new StorageObject();
        o.setOwner(owner);
        o.setObjectKey(key);
        o.setProvider(provider.name());
        o.setContentType(contentType(detected));
        o.setSizeBytes(bytes.length);
        o.setChecksum(sha256(bytes));
        o.setState(StorageState.ACTIVE);
        return objects.save(o);
    }

    @Transactional
    public void markDeleted(StorageObject o) {
        o.setState(StorageState.DELETED);
        objects.save(o);
        provider.delete(o.getObjectKey());
    }

    @Transactional(readOnly = true)
    public StoredFile load(String key) {
        if (!SAFE_KEY.matcher(key).matches()) {
            throw ApiException.notFound("File");
        }
        StorageObject o = objects.findByObjectKeyAndState(key, StorageState.ACTIVE).orElseThrow(() -> ApiException.notFound("File"));
        Resource r = provider.get(key);
        if (!r.exists()) {
            throw ApiException.notFound("File");
        }
        return new StoredFile(r, o.getContentType());
    }

    public record StoredFile(Resource resource, String contentType) {}

    private static String detectType(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    private static boolean matches(String ext, String detected) {
        return ext.equals(detected) || (ext.equals("jpeg") && detected.equals("jpg"));
    }

    private static String contentType(String detected) {
        return switch (detected) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "image/webp";
        };
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
