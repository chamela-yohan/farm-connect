package lk.farmconnect.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String getPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            log.debug("Generating presigned URL for bucket: {}, key: {}", bucketName, objectKey);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)  // farmconnect-products
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Generated presigned URL: {}", presignedUrl);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for bucket: {}, key: {}", bucketName, objectKey, e);
            return null;
        }
    }
}