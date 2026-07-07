package lk.farmconnect.common.service;

import lk.farmconnect.common.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    // File type categories
    public enum FileType {
        IMAGE,
        VIDEO,
        DOCUMENT
    }

    // Validation rules per file type
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;        // 5MB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;       // 50MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;    // 10MB

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4", "video/quicktime"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = List.of(
            "application/pdf"
    );

    
    // UPLOAD FILE (Returns S3 KEY)
    public String uploadFile(MultipartFile file, String folder, FileType fileType) {
        validateFile(file, fileType);
        String key = generateUniqueKey(file.getOriginalFilename(), folder);

        try {
            log.info("Uploading {} to S3: bucket={}, key={}, size={}",
                    fileType, bucketName, key, file.getSize());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            return key;

        } catch (IOException e) {
            log.error("IO Exception during upload", e);
            throw new FileUploadException("Failed to process file stream");
        } catch (Exception e) {
            log.error("S3 Upload failed for key: {}", key, e);
            throw new FileUploadException("Failed to upload file to storage");
        }
    }

    // Overload for backward compatibility (defaults to IMAGE)
    public String uploadFile(MultipartFile file, String folder) {
        return uploadFile(file, folder, FileType.IMAGE);
    }

    
    // GENERATE PRESIGNED URL
    public String getPresignedUrl(String objectKey) {
        return getPresignedUrl(objectKey, Duration.ofHours(1));
    }

    public String getPresignedUrl(String objectKey, Duration duration) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", objectKey, e);
            return null;
        }
    }

    
    // DELETE FILE
    public void deleteFile(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            log.info("Deleted file from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", key, e);
        }
    }

    
    // VALIDATION
    private void validateFile(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new FileUploadException("File content type is missing");
        }

        switch (fileType) {
            case IMAGE:
                if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                    throw new FileUploadException("Invalid image format. Allowed: JPEG, PNG, WEBP");
                }
                if (file.getSize() > MAX_IMAGE_SIZE) {
                    throw new FileUploadException("Image file size exceeds the 5MB limit");
                }
                break;

            case VIDEO:
                if (!ALLOWED_VIDEO_TYPES.contains(contentType)) {
                    throw new FileUploadException("Invalid video format. Allowed: MP4, MOV");
                }
                if (file.getSize() > MAX_VIDEO_SIZE) {
                    throw new FileUploadException("Video file size exceeds the 50MB limit");
                }
                break;

            case DOCUMENT:
                if (!ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
                    throw new FileUploadException("Invalid document format. Allowed: PDF");
                }
                if (file.getSize() > MAX_DOCUMENT_SIZE) {
                    throw new FileUploadException("Document file size exceeds the 10MB limit");
                }
                break;
        }
    }

    private String generateUniqueKey(String originalFilename, String folder) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }
}