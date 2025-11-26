package com.zhk.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志审计实体类
 *
 * @author shigure
 */
@Data
@TableName("audit_log")
public class AuditLog {
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID（可为空，如系统操作）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作用户名（冗余字段，便于查询）
     */
    private String username;

    /**
     * 用户角色：TENANT/OWNER/OPERATOR
     */
    private String role;

    /**
     * 操作类型：LOGIN/LOGOUT/CREATE_ORDER/PAYMENT/APPEAL_RESOLVE/USER_FREEZE/USER_UNFREEZE/ACCOUNT_CREATE/ACCOUNT_UPDATE/ACCOUNT_DELETE等
     */
    private String action;

    /**
     * 资源类型：USER/ORDER/ACCOUNT/APPEAL/PAYMENT等
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 资源ID
     */
    @TableField("resource_id")
    private Long resourceId;

    /**
     * 操作描述
     */
    private String description;

    /**
     * HTTP请求方法：GET/POST/PUT/DELETE等
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求路径
     */
    @TableField("request_path")
    private String requestPath;

    /**
     * 请求参数（JSON格式）
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应状态码
     */
    @TableField("response_status")
    private Integer responseStatus;

    /**
     * 客户端IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 用户代理（浏览器信息）
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 操作是否成功：1-成功 0-失败
     */
    private Integer success;

    /**
     * 错误信息（失败时记录）
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("execution_time")
    private Integer executionTime;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

