package com.playvora.playvora_api.community.services.impl;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.community.services.IFileUploadService;
import com.playvora.playvora_api.community.services.IS3Service;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService implements IFileUploadService {

    private final IS3Service s3Service;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public Map<String, String> uploadFile(MultipartFile file, String userId) {
        validateFile(file);

        String sanitizedOriginalName = sanitizeOriginalName(file.getOriginalFilename());
        String fileName = s3Service.generateUniqueFileName(sanitizedOriginalName);
        String folder = "users/" + userId;

        String fileKey = s3Service.uploadFile(file, folder, fileName);
        String url = s3Service.getFileUrl(fileKey);

        Map<String, String> fileData = new HashMap<>();
        fileData.put("fileKey", fileKey);
        fileData.put("fileName", extractFileName(fileKey));
        fileData.put("url", url);
        fileData.put("originalFileName", sanitizedOriginalName);

        log.info("File uploaded successfully to S3: {}", fileKey);
        return fileData;
    }

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        String sanitizedOriginalName = sanitizeOriginalName(file.getOriginalFilename());
        String fileName = s3Service.generateUniqueFileName(sanitizedOriginalName);

        String fileKey = s3Service.uploadFile(file, folder, fileName);
        String url = s3Service.getFileUrl(fileKey);
        log.info("Image uploaded successfully to S3: {}", fileKey);
        return url;
    }

    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String fileKey = extractFileKeyFromUrl(imageUrl);
        if (fileKey != null) {
            s3Service.deleteFile(fileKey);
            log.info("File deleted successfully from S3: {}", fileKey);
        }
    }

    private String extractFileKeyFromUrl(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            return normalized.isBlank() ? null : normalized;
        } catch (Exception e) {
            log.error("Error extracting file key from URL: {}", e.getMessage());
        }
        return null;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("File has no name");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        boolean isValidExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new BadRequestException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String sanitizeOriginalName(String originalFilename) {
        if (originalFilename == null) {
            return "unknown-file";
        }
        String sanitized = originalFilename.trim().replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "");
        return sanitized.isEmpty() ? "unknown-file" : sanitized;
    }

    private String extractFileName(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return fileKey;
        }
        int lastSlash = fileKey.lastIndexOf('/');
        return lastSlash >= 0 ? fileKey.substring(lastSlash + 1) : fileKey;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
}
