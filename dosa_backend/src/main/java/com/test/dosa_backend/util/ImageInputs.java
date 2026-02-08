package com.test.dosa_backend.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Helpers for turning user-supplied image inputs (URLs + uploaded files) into the shape
 * expected by OpenAI "input_image" (either an https URL or a data: URL with base64).
 */
public final class ImageInputs {

    private ImageInputs() {}

    public static final int MAX_IMAGES = 4;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif"
    );

    public static List<String> normalizeImageInputs(List<String> imageInputs) {
        if (imageInputs == null || imageInputs.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        for (String s : imageInputs) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            out.add(t);
        }

        if (out.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Too many images (max " + MAX_IMAGES + ").");
        }

        for (String url : out) {
            // URLs must be https, or a data URL produced from uploaded files.
            if (url.startsWith("https://")) continue;
            if (url.startsWith("data:image/")) continue;
            throw new IllegalArgumentException("imageUrls must be https URLs (or uploaded image files).");
        }

        return out;
    }

    public static List<String> filesToDataUrls(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            out.add(toDataUrl(f));
        }
        return out;
    }

    public static String toDataUrl(MultipartFile file) {
        String contentType = (file == null) ? null : file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Image content type is missing.");
        }
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image content type: " + contentType);
        }
        long size = file.getSize();
        if (size > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image is too large (max " + (MAX_IMAGE_BYTES / (1024 * 1024)) + "MB).");
        }
        try {
            byte[] bytes = file.getBytes();
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + contentType + ";base64," + b64;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded image.", e);
        }
    }
}

