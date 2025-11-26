package com.zhk.infrastructure.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO配置属性
 *
 * @author shigure
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhk.minio")
public class MinIOProperties {
    /**
     * MinIO服务端点
     */
    private String endpoint = "http://localhost:9000";

    /**
     * 访问密钥
     */
    private String accessKey = "minioadmin";

    /**
     * 秘密密钥
     */
    private String secretKey = "minioadmin123";

    /**
     * 存储桶名称
     */
    private String bucketName = "zhk-evidence";
}

