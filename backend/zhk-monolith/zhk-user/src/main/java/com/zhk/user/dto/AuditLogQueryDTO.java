package com.zhk.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志审计查询DTO
 *
 * @author shigure
 */
@Data
public class AuditLogQueryDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
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
     * 是否成功：1-成功 0-失败
     */
    private Integer success;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}

