package com.zhk.order.service;

import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;

/**
 * 支付服务接口
 *
 * @author shigure
 */
public interface PaymentService {
    /**
     * 创建支付
     */
    PaymentVO createPayment(Long userId, CreatePaymentDTO dto);

    /**
     * 查询支付状态
     */
    PaymentVO getPaymentStatus(Long paymentId, Long userId);

    /**
     * 支付成功回调（更新订单状态）
     */
    void onPaymentSuccess(Long paymentId);
}

