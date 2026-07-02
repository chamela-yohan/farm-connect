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

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "video/mp4"
    );

    
    // UPLOAD FILE (Returns the S3 KEY, not the URL)
    
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);
        String key = generateUniqueKey(file.getOriginalFilename(), folder);

        try {
            log.info("Uploading file to S3: bucket={}, key={}, size={}", bucketName, key, file.getSize());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            // Return the KEY, not a presigned URL.
            // The service layer will generate presigned URLs on the fly when needed.
            return key;

        } catch (IOException e) {
            log.error("IO Exception during upload", e);
            throw new FileUploadException("Failed to process file stream");
        } catch (Exception e) {
            log.error("S3 Upload failed for key: {}", key, e);
            throw new FileUploadException("Failed to upload file to storage");
        }
    }

    
    // GENERATE PRESIGNED URL (Used on the fly)
    public String getPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            // Strict 1-hour expiration for security
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", objectKey, e);
            return null;
        }
    }

    public void deleteFile(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName).key(key).build());
            log.info("Deleted file from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", key, e);
        }
    }

    // --- Private Validation ---
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileUploadException("File cannot be empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new FileUploadException("File size exceeds 5MB limit");
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) throw new FileUploadException("Invalid file type.");
    }

    private String generateUniqueKey(String originalFilename, String folder) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }
}