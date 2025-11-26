package com.zhk.order.controller;

import com.zhk.common.security.JwtUtil;
import com.zhk.common.web.Result;
import com.zhk.infrastructure.minio.service.MinIOService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * @author shigure
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final MinIOService minIOService;
    private final JwtUtil jwtUtil;

    /**
     * 上传文件（申诉证据）
     *
     * @param file 文件
     * @param request HTTP请求
     * @return 文件URL
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        // 验证用户登录
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未授权，请先登录");
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "Token无效");
            }

            // 验证文件
            if (file.isEmpty()) {
                return Result.error(400, "文件不能为空");
            }

            // 限制文件大小（10MB）
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return Result.error(400, "文件大小不能超过10MB");
            }

            // 验证文件类型（仅允许图片和视频）
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                return Result.error(400, "仅支持图片和视频文件");
            }

            // 生成文件路径：evidence/{userId}/{date}/{uuid}.{ext}
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = String.format("evidence/%d/%s/%s%s", 
                    userId, datePath, UUID.randomUUID().toString(), extension);

            // 上传文件
            try (InputStream inputStream = file.getInputStream()) {
                String fileUrl = minIOService.uploadFile(objectName, inputStream, contentType);
                log.info("文件上传成功: userId={}, objectName={}, url={}", userId, objectName, fileUrl);
                return Result.success("文件上传成功", fileUrl);
            }

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 从请求头获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

