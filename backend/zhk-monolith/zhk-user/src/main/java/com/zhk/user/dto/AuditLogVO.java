package com.zhk.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志审计VO
 *
 * @author shigure
 */
@Data
public class AuditLogVO {
    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 操作描述
     */
    private String description;

    /**
     * HTTP请求方法
     */
    private String requestMethod;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 客户端IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 操作是否成功
     */
    private Integer success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

