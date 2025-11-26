package com.zhk.infrastructure.minio.service;

import java.io.InputStream;

/**
 * MinIO服务接口
 *
 * @author shigure
 */
public interface MinIOService {
    /**
     * 上传文件
     *
     * @param objectName 对象名称（文件路径）
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @return 文件访问URL
     */
    String uploadFile(String objectName, InputStream inputStream, String contentType);

    /**
     * 删除文件
     *
     * @param objectName 对象名称（文件路径）
     */
    void deleteFile(String objectName);

    /**
     * 获取文件访问URL（预签名URL，有效期7天）
     *
     * @param objectName 对象名称（文件路径）
     * @return 文件访问URL
     */
    String getFileUrl(String objectName);

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称（文件路径）
     * @return 是否存在
     */
    boolean fileExists(String objectName);
}

