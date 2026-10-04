package com.bookforward.controller;

import com.bookforward.storage.StorageService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final StorageService storage;

    @GetMapping("/{key:.+}")
    public ResponseEntity<Resource> get(@PathVariable String key) {
        var f = storage.load(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(f.resource());
    }
}
