package com.zhk.infrastructure.minio.service.impl;

import com.zhk.infrastructure.minio.config.MinIOProperties;
import com.zhk.infrastructure.minio.service.MinIOService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private MinioClient minioClient; // 可能为 null（如果配置不存在）
    
    private final MinIOProperties minIOProperties;

    @Override
    public String uploadFile(String objectName, InputStream inputStream, String contentType) {
        if (minioClient == null) {
            log.warn("MinIO 客户端未配置，无法上传文件: {}", objectName);
            throw new RuntimeException("MinIO 服务未配置，无法上传文件");
        }
        
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
        if (minioClient == null) {
            log.warn("MinIO 客户端未配置，无法删除文件: {}", objectName);
            throw new RuntimeException("MinIO 服务未配置，无法删除文件");
        }
        
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
        if (minioClient == null) {
            log.warn("MinIO 客户端未配置，无法获取文件URL: {}", objectName);
            throw new RuntimeException("MinIO 服务未配置，无法获取文件URL");
        }
        
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
        if (minioClient == null) {
            log.warn("MinIO 客户端未配置，无法检查文件存在性: {}", objectName);
            return false;
        }
        
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

