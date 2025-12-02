package com.playvora.playvora_api.community.services.impl;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.community.services.IS3Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service implements IS3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${aws.s3.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${aws.s3.compression.threshold-bytes:307200}") // 300KB default
    private long compressionThreshold;

    @Override
    public String uploadFile(MultipartFile file, String folder, String fileName) {
        try {
            String normalizedFolder = normalizeFolder(folder);
            String fileKey = buildFileKey(normalizedFolder, fileName);

            byte[] payload = file.getBytes();
            String contentType = file.getContentType();
            boolean compressed = false;

            if (shouldCompress(file)) {
                try (InputStream inputStream = file.getInputStream()) {
                    payload = compressFile(inputStream);
                }
                fileKey = fileKey + ".gz";
                contentType = contentType != null ? contentType : "application/octet-stream";
                compressed = true;
                log.debug("Compressed file {} (original size: {} bytes, compressed size: {} bytes)",
                        fileName, file.getSize(), payload.length);
            }

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey);

            if (contentType != null) {
                requestBuilder.contentType(contentType);
            }
            if (compressed) {
                requestBuilder.contentEncoding("gzip");
            }

            s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(payload));
            log.info("Uploaded file to S3: {}", fileKey);
            return fileKey;
        } catch (IOException ex) {
            log.error("Error uploading file to S3", ex);
            throw new BadRequestException("Failed to upload file: " + ex.getMessage());
        } catch (S3Exception ex) {
            log.error("AWS S3 error while uploading file: {}", ex.awsErrorDetails().errorMessage());
            throw new BadRequestException("Failed to upload file to S3");
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted file from S3: {}", fileKey);
        } catch (NoSuchKeyException ex) {
            log.warn("Attempted to delete non-existent file: {}", fileKey);
        } catch (S3Exception ex) {
            log.error("AWS S3 error while deleting file: {}", ex.awsErrorDetails().errorMessage());
            throw new BadRequestException("Failed to delete file from S3");
        }
    }

    @Override
    public String getFileUrl(String fileKey) {
        try {
            return s3Client.utilities()
                    .getUrl(builder -> builder.bucket(bucketName).key(fileKey))
                    .toExternalForm();
        } catch (S3Exception ex) {
            log.error("AWS S3 error while generating file URL: {}", ex.awsErrorDetails().errorMessage());
            throw new BadRequestException("Failed to generate file URL");
        }
    }

    @Override
    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            log.error("AWS S3 error while checking file existence: {}", ex.awsErrorDetails().errorMessage());
            return false;
        }
    }

    @Override
    public String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID() + extension;
    }

    @Override
    public byte[] downloadFile(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest)) {
            return response.readAllBytes();
        } catch (NoSuchKeyException ex) {
            log.error("File not found in S3: {}", fileKey);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        } catch (IOException ex) {
            log.error("IO error downloading file from S3", ex);
            throw new BadRequestException("Failed to download file: " + ex.getMessage());
        } catch (S3Exception ex) {
            log.error("AWS S3 error while downloading file: {}", ex.awsErrorDetails().errorMessage());
            throw new BadRequestException("Failed to download file from S3");
        }
    }

    @Override
    public Map<String, Object> getFileMetadata(String fileKey) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        try {
            HeadObjectResponse response = s3Client.headObject(headRequest);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("size", response.contentLength());
            metadata.put("lastModified", response.lastModified());
            metadata.put("type", response.contentType());
            metadata.put("contentEncoding", response.contentEncoding());
            metadata.put("metadata", response.metadata());

            return metadata;
        } catch (NoSuchKeyException e) {
            log.error("File not found: {}", fileKey, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        } catch (S3Exception ex) {
            log.error("AWS S3 error while retrieving metadata: {}", ex.awsErrorDetails().errorMessage());
            throw new BadRequestException("Failed to retrieve file metadata");
        }
    }

    @Override
    public String generatePresignedUrl(String fileKey, long expirationMs) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMillis(expirationMs))
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception ex) {
            log.error("AWS S3 error while generating presigned URL: {}", ex.awsErrorDetails().errorMessage());
            throw new BadRequestException("Failed to generate presigned URL");
        }
    }

    public List<String> listUserFiles(String prefix) {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
        List<String> fileKeys = new ArrayList<>();

        for (S3Object s3Object : response.contents()) {
            fileKeys.add(s3Object.key());
        }

        return fileKeys;
    }

    private boolean shouldCompress(MultipartFile file) {
        if (!compressionEnabled) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType != null && (
                contentType.contains("zip")
                        || contentType.contains("gzip")
                        || contentType.contains("compressed")
                        || contentType.contains("image/jpeg")
                        || contentType.contains("image/png")
                        || contentType.contains("image/gif")
                        || contentType.contains("video/")
                        || contentType.contains("audio/")
                        || contentType.contains("application/pdf")
        )) {
            return false;
        }

        return file.getSize() > compressionThreshold;
    }

    private byte[] compressFile(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = input.read(buffer)) != -1) {
                gzipOutputStream.write(buffer, 0, len);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "";
        }
        String normalized = folder.replace("\\", "/");
        normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        normalized = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        return normalized;
    }

    private String buildFileKey(String folder, String fileName) {
        return folder == null || folder.isBlank() ? fileName : folder + "/" + fileName;
    }
}
