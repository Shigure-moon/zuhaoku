package com.zhk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租赁订单实体类
 *
 * @author shigure
 */
@Data
@TableName("lease_order")
public class LeaseOrder {
    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号ID
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 租客用户ID
     */
    @TableField("tenant_uid")
    private Long tenantUid;

    /**
     * 租期开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 租期结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 实际结束时间
     */
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

    /**
     * 租金
     */
    private BigDecimal amount;

    /**
     * 押金
     */
    private BigDecimal deposit;

    /**
     * 订单状态：paying-待支付, leasing-租赁中, closed-已完成, appeal-申诉中, cancelled-已取消
     */
    private String status;

    /**
     * 还号证据哈希（SHA-256）
     */
    @TableField("evidence_hash")
    private String evidenceHash;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

