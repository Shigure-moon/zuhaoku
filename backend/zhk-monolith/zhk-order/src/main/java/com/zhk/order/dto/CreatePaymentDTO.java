package com.zhk.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建支付请求 DTO
 *
 * @author shigure
 */
@Data
public class CreatePaymentDTO {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "支付方式不能为空")
    private String paymentType; // wechat, alipay
}

