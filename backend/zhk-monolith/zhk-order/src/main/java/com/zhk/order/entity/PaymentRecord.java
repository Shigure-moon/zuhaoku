package com.zhk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 *
 * @author shigure
 */
@Data
@TableName("payment_record")
public class PaymentRecord {
    /**
     * 支付记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 支付方式：wechat-微信, alipay-支付宝
     */
    @TableField("payment_type")
    private String paymentType;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 第三方交易号
     */
    @TableField("transaction_id")
    private String transactionId;

    /**
     * 支付状态：pending-待支付, success-成功, failed-失败, refunded-已退款
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 支付完成时间
     */
    @TableField("paid_at")
    private LocalDateTime paidAt;
}

