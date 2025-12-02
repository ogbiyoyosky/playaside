package com.playvora.playvora_api.community.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IFileUploadService {
    String uploadImage(MultipartFile file, String folder);
    void deleteImage(String imageUrl);
    Map<String, String> uploadFile(MultipartFile file, String userId);
}
