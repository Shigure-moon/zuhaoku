package com.zhk.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单实体类
 *
 * @author shigure
 */
@Data
@TableName("blacklist")
public class Blacklist {
    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型：IP/DEVICE/PHONE/USER
     */
    private String type;

    /**
     * 黑名单值（IP地址/设备指纹/手机号/用户ID）
     */
    private String value;

    /**
     * 加入黑名单原因
     */
    private String reason;

    /**
     * 风险等级：1-低 2-中 3-高
     */
    @TableField("risk_level")
    private Integer riskLevel;

    /**
     * 状态：1-生效 0-失效
     */
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 过期时间（NULL表示永久）
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}

