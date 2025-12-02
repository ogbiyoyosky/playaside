package com.playvora.playvora_api.controllers;

import com.playvora.playvora_api.common.dto.ErrorResponse;
import com.playvora.playvora_api.common.dto.SuccessResponse;
import com.playvora.playvora_api.common.services.FileService;
import com.playvora.playvora_api.files.dtos.FileResourcePayload;
import com.playvora.playvora_api.files.dtos.UserFileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Manage user file uploads and retrieval from S3")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("isAuthenticated()")
public class FileController {

    private final FileService fileService;

    @Operation(summary = "Upload a file to S3", description = "Uploads a file to the user's folder in S3 and returns a pre-signed URL for immediate access")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error uploading file", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponse<Map<String, Object>>> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            Principal connectedUser) {
        Map<String, Object> fileInfo = fileService.uploadFile(file, connectedUser);
        return ResponseEntity.ok(SuccessResponse.of(fileInfo, "File uploaded successfully"));
    }

    @Operation(summary = "List user files", description = "Returns a list of all files uploaded by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files listed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<SuccessResponse<List<UserFileResponse>>> listUserFiles(Principal connectedUser) {
        List<UserFileResponse> files = fileService.listUserFiles(connectedUser);
        return ResponseEntity.ok(SuccessResponse.of(files, "Files retrieved successfully"));
    }

    @Operation(summary = "Delete a file", description = "Deletes a specific file from the user's folder in S3")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{fileName}")
    public ResponseEntity<SuccessResponse<Void>> deleteFile(
            @Parameter(description = "File name to delete", required = true, example = "abc-123_file.jpg")
            @PathVariable String fileName,
            Principal connectedUser) {
        fileService.deleteFile(fileName, connectedUser);
        return ResponseEntity.ok(SuccessResponse.<Void>of("File deleted successfully"));
    }

    @Operation(summary = "Get a pre-signed URL image", description = "Generates an image from a pre-signed URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - file belongs to another user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })


    @GetMapping("/{userId}/{fileName}")
    public ResponseEntity<Resource> getFileImage(
            @Parameter(description = "File name to get image for", required = true, example = "abc-123_file.jpg")
            @PathVariable String fileName,
            @Parameter(description = "userId of the file to get image for", required = true, example = "1")
            @PathVariable String userId) {
        FileResourcePayload payload = fileService.getFileImage(userId, fileName    );
        MediaType responseMediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (payload.contentType() != null) {
            try {
                responseMediaType = MediaType.parseMediaType(payload.contentType());
            } catch (Exception ignored) {
                responseMediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return ResponseEntity.ok()
                .contentType(responseMediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.originalFileName() + "\"")
                .body(payload.resource());
    }

    @Operation(summary = "Get a pre-signed URL for a file", description = "Generates a temporary pre-signed URL for accessing a specific file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pre-signed URL generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{fileName}/presigned-url")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getPresignedUrl(
            @Parameter(description = "File name to generate a pre-signed URL for", required = true, example = "abc-123_file.jpg")
            @PathVariable String fileName,
            Principal connectedUser) {
        Map<String, Object> presignedUrl = fileService.getPresignedUrl(fileName, connectedUser);
        return ResponseEntity.ok(SuccessResponse.of(presignedUrl, "Pre-signed URL generated successfully"));
    }

    @Operation(summary = "Get a metadata for a file", description = "Gets the metadata of a specific file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{fileName}/metadata")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getFileMetadata(
            @Parameter(description = "File name to get metadata for", required = true, example = "abc-123_file.jpg")
            @PathVariable String fileName,
            Principal connectedUser) {
        Map<String, Object> metadata = fileService.getFileMetadata(fileName, connectedUser);
        return ResponseEntity.ok(SuccessResponse.of(metadata, "File metadata retrieved successfully"));
    }
}
