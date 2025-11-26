package com.zhk.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常行为记录实体类
 *
 * @author shigure
 */
@Data
@TableName("abnormal_behavior")
public class AbnormalBehavior {
    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 行为类型：FREQUENT_CANCEL/PAYMENT_FAILURE/ACCOUNT_ABUSE/MULTI_LOCATION_LOGIN等
     */
    @TableField("behavior_type")
    private String behaviorType;

    /**
     * 行为描述
     */
    private String description;

    /**
     * 风险评分：0-100
     */
    @TableField("risk_score")
    private Integer riskScore;

    /**
     * 资源类型：ORDER/ACCOUNT/PAYMENT等
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 资源ID
     */
    @TableField("resource_id")
    private Long resourceId;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 设备指纹
     */
    @TableField("device_fingerprint")
    private String deviceFingerprint;

    /**
     * 处理状态：0-待处理 1-已处理 2-已忽略
     */
    private Integer status;

    /**
     * 处理人ID
     */
    @TableField("handled_by")
    private Long handledBy;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

