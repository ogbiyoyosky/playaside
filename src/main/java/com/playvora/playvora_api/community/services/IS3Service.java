package com.playvora.playvora_api.community.services;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface IS3Service {
    String uploadFile(MultipartFile file, String folder, String fileName);

    void deleteFile(String fileKey);

    String getFileUrl(String fileKey);

    boolean fileExists(String fileKey);

    String generateUniqueFileName(String originalFilename);

    byte[] downloadFile(String fileKey);

    Map<String, Object> getFileMetadata(String fileKey);

    String generatePresignedUrl(String fileKey, long expirationMs);
}
