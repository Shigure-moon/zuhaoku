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

            return client;
        } catch (Exception e) {
            log.error("初始化MinIO客户端失败", e);
            throw new RuntimeException("初始化MinIO客户端失败", e);
        }
    }
}

