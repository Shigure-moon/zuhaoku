package com.zhk.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付展示 VO
 *
 * @author shigure
 */
@Data
public class PaymentVO {
    private Long id;
    private Long orderId;
    private String paymentType;
    private BigDecimal amount;
    private String transactionId;
    private String status; // pending, success, failed, refunded
    private String paymentUrl;
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}

