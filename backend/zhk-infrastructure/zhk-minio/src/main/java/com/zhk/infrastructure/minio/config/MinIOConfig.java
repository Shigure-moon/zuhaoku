package com.zhk.infrastructure.minio.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 *
 * @author shigure
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinIOConfig {

    private final MinIOProperties minIOProperties;

    @Bean
    public MinioClient minioClient() {
        // 验证配置 - 如果配置不完整，返回 null（不创建客户端）
        if (minIOProperties.getEndpoint() == null || minIOProperties.getEndpoint().trim().isEmpty()) {
            log.warn("MinIO endpoint 未配置，MinIO 功能将不可用。请检查配置：zhk.minio.endpoint");
            return null;
        }
        
        if (minIOProperties.getAccessKey() == null || minIOProperties.getAccessKey().trim().isEmpty()) {
            log.warn("MinIO accessKey 未配置，MinIO 功能将不可用。请检查配置：zhk.minio.access-key");
            return null;
        }
        
        if (minIOProperties.getSecretKey() == null || minIOProperties.getSecretKey().trim().isEmpty()) {
            log.warn("MinIO secretKey 未配置，MinIO 功能将不可用。请检查配置：zhk.minio.secret-key");
            return null;
        }
        
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(minIOProperties.getEndpoint())
                    .credentials(minIOProperties.getAccessKey(), minIOProperties.getSecretKey())
                    .build();

            // 检查并创建存储桶
            boolean found = client.bucketExists(io.minio.BucketExistsArgs.builder()
                    .bucket(minIOProperties.getBucketName())
                    .build());

            if (!found) {
                client.makeBucket(io.minio.MakeBucketArgs.builder()
                        .bucket(minIOProperties.getBucketName())
                        .build());
                log.info("创建MinIO存储桶: {}", minIOProperties.getBucketName());
            } else {
                log.info("MinIO存储桶已存在: {}", minIOProperties.getBucketName());
            }

            log.info("✅ MinIO客户端初始化成功: endpoint={}, bucket={}", 
                    minIOProperties.getEndpoint(), minIOProperties.getBucketName());
            return client;
        } catch (Exception e) {
            log.error("初始化MinIO客户端失败，MinIO 功能将不可用", e);
            // 返回 null 而不是抛出异常，允许应用在没有 MinIO 的情况下启动
            return null;
        }
    }
}

