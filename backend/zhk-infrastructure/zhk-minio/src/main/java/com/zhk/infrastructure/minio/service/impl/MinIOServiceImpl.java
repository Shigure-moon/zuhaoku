package com.zhk.infrastructure.minio.service.impl;

import com.zhk.infrastructure.minio.config.MinIOProperties;
import com.zhk.infrastructure.minio.service.MinIOService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOServiceImpl implements MinIOService {

    private final MinioClient minioClient;
    private final MinIOProperties minIOProperties;

    @Override
    public String uploadFile(String objectName, InputStream inputStream, String contentType) {
        try {
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, -1, 10485760) // 10MB分片大小
                            .contentType(contentType)
                            .build()
            );

            // 返回文件访问URL
            return getFileUrl(objectName);
        } catch (Exception e) {
            log.error("上传文件失败: {}", objectName, e);
            throw new RuntimeException("上传文件失败: " + objectName, e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("删除文件成功: {}", objectName);
        } catch (Exception e) {
            log.error("删除文件失败: {}", objectName, e);
            throw new RuntimeException("删除文件失败: " + objectName, e);
        }
    }

    @Override
    public String getFileUrl(String objectName) {
        try {
            // 生成预签名URL，有效期7天
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
            return url;
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", objectName, e);
            throw new RuntimeException("获取文件URL失败: " + objectName, e);
        }
    }

    @Override
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("检查文件存在性失败: {}", objectName, e);
            throw new RuntimeException("检查文件存在性失败: " + objectName, e);
        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", objectName, e);
            throw new RuntimeException("检查文件存在性失败: " + objectName, e);
        }
    }
}

