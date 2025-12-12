package com.playvora.playvora_api.common.services;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.community.services.IS3Service;
import com.playvora.playvora_api.files.dtos.FileResourcePayload;
import com.playvora.playvora_api.files.dtos.UserFileResponse;
import com.playvora.playvora_api.files.entities.UserFile;
import com.playvora.playvora_api.files.repositories.UserFileRepository;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {
    private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB
    private static final String USER_FOLDER_PREFIX = "users";

    private final IS3Service s3Service;
    private final UserRepository userRepository;
    private final UserFileRepository userFileRepository;

    public Map<String, Object> uploadFile(MultipartFile file, Principal principal) {
        validateFile(file);
        User user = resolveUser(principal);

        String sanitizedOriginalName = sanitizeOriginalFilename(file.getOriginalFilename());
        String folder = buildUserFolder(user);
        String uniqueFileName = s3Service.generateUniqueFileName(sanitizedOriginalName);
        String fileKey = s3Service.uploadFile(file, folder, uniqueFileName);
        String storedFileName = extractFileNameFromKey(fileKey);
        String fileUrl = s3Service.getFileUrl(fileKey);

        UserFile userFile = userFileRepository.save(
                UserFile.builder()
                        .user(user)
                        .fileName(storedFileName)
                        .originalFileName(sanitizedOriginalName)
                        .contentType(file.getContentType())
                        .size(file.getSize())
                        .fileUrl(fileUrl)
                        .fileKey(fileKey)
                        .uploadedAt(OffsetDateTime.now())
                        .build());

        Map<String, Object> response = new HashMap<>();
        response.put("fileName", userFile.getFileName());
        response.put("originalFileName", userFile.getOriginalFileName());
        response.put("url", userFile.getFileUrl());
        response.put("contentType", userFile.getContentType());
        response.put("size", userFile.getSize());
        response.put("uploadedAt", userFile.getUploadedAt());
        response.put("fileKey", userFile.getFileKey());
        return response;
    }

    public List<UserFileResponse> listUserFiles(Principal principal) {
        User user = resolveUser(principal);
        return userFileRepository.findAllByUserOrderByUploadedAtDesc(user).stream()
                .map(this::toUserFileResponse)
                .collect(Collectors.toList());
    }

    public void deleteFile(String fileName, Principal principal) {
        User user = resolveUser(principal);
        UserFile userFile = userFileRepository.findByUserAndFileName(user, fileName)
                .orElseThrow(() -> new BadRequestException("File not found: " + fileName));

        s3Service.deleteFile(userFile.getFileKey());
        userFileRepository.delete(userFile);
    }

    public FileResourcePayload getFileImage(String userId, String fileName) {
        UUID userIdUUID = UUID.fromString(userId);

        UserFile userFile = userFileRepository.findByUserIdAndFileName(userIdUUID, fileName)
                .orElseThrow(() -> new BadRequestException("File not found: " + fileName));

        if (!s3Service.fileExists(userFile.getFileKey())) {
            throw new BadRequestException("Stored file no longer exists in S3");
        }

        byte[] data = s3Service.downloadFile(userFile.getFileKey());
        Resource resource = new ByteArrayResource(data);
        return new FileResourcePayload(resource, userFile.getContentType(), userFile.getOriginalFileName());
    }

    public Map<String, Object> getFileMetadata(String fileName, Principal principal) {
        User user = resolveUser(principal);
        UserFile userFile = userFileRepository.findByUserAndFileName(user, fileName)
                .orElseThrow(() -> new BadRequestException("File not found: " + fileName));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", userFile.getFileName());
        metadata.put("originalFileName", userFile.getOriginalFileName());
        metadata.put("contentType", userFile.getContentType());
        metadata.put("size", userFile.getSize());
        metadata.put("uploadedAt", userFile.getUploadedAt());
        metadata.put("url", userFile.getFileUrl());
        metadata.put("s3Metadata", s3Service.getFileMetadata(userFile.getFileKey()));
        return metadata;
    }

    public Map<String, Object> getPresignedUrl(String fileName, Principal principal) {
        User user = resolveUser(principal);
        UserFile userFile = userFileRepository.findByUserAndFileName(user, fileName)
                .orElseThrow(() -> new BadRequestException("File not found: " + fileName));

        Map<String, Object> response = new HashMap<>();
        response.put("fileName", userFile.getFileName());
        response.put("url", s3Service.generatePresignedUrl(userFile.getFileKey(), 15 * 60 * 1000L));
        return response;
    }

    private User resolveUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds maximum allowed size of 20 MB");
        }
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null) {
            return "unknown-file";
        }
        String sanitized = originalFilename.trim().replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "");
        return sanitized.isEmpty() ? "unknown-file" : sanitized;
    }

    private String buildUserFolder(User user) {
        return USER_FOLDER_PREFIX + "/" + user.getId();
    }

    // Removed parseUserId - IDs are now String UUIDs, no parsing needed

    private String extractFileNameFromKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return fileKey;
        }
        int lastSlash = fileKey.lastIndexOf('/');
        return lastSlash >= 0 ? fileKey.substring(lastSlash + 1) : fileKey;
    }

    private UserFileResponse toUserFileResponse(UserFile userFile) {
        return UserFileResponse.builder()
                .fileName(userFile.getFileName())
                .originalFileName(userFile.getOriginalFileName())
                .contentType(userFile.getContentType())
                .size(userFile.getSize())
                .url(userFile.getFileUrl())
                .uploadedAt(userFile.getUploadedAt())
                .build();
    }
}

