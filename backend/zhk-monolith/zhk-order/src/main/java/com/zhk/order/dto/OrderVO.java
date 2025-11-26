package com.zhk.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单展示 VO
 *
 * @author shigure
 */
@Data
public class OrderVO {
    private Long id;
    private String orderNo; // 订单号（使用 id 生成）
    private Long accountId;
    private String accountTitle; // 账号标题
    private String accountDescription; // 账号描述
    private String gameName;
    private Long tenantUid;
    private Long tenantId; // 前端字段名（与 tenantUid 相同）
    private String tenantNickname;
    private Long ownerUid;
    private Long ownerId; // 前端字段名（与 ownerUid 相同）
    private String ownerNickname;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;
    private BigDecimal amount; // 租金
    private BigDecimal deposit; // 押金
    private BigDecimal totalAmount; // 总金额（租金 + 押金）
    private String status; // 前端状态：PENDING_PAYMENT, PAID, ACTIVE, EXPIRED, RETURNED, CANCELLED, DISPUTED, COMPLETED
    private Integer duration; // 租期时长（分钟）
    private String username; // 账号（解密后，仅订单详情返回）
    private String password; // 密码（解密后，仅订单详情返回）
    
    // 支付信息
    private String paymentType; // 支付方式：wechat, alipay
    private String paymentStatus; // 支付状态：pending, success, failed, refunded
    private LocalDateTime paymentTime; // 支付时间
    private String transactionId; // 第三方交易号
    
    // 账号详细信息（订单详情返回）
    private Integer accountLevel; // 账号等级
    private String accountSkins; // 账号皮肤（JSON字符串）
    
    // 剩余时间（租赁中订单）
    private Long remainingMinutes; // 剩余分钟数
    
    // 订单时间线
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

