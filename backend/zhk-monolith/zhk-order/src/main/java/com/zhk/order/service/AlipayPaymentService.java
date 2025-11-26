package com.zhk.order.service;

import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;

import java.util.Map;

/**
 * 支付宝支付服务接口
 *
 * @author shigure
 */
public interface AlipayPaymentService {
    /**
     * 创建支付宝支付
     *
     * @param userId 用户ID
     * @param dto 创建支付DTO
     * @return 支付VO
     */
    PaymentVO createPayment(Long userId, CreatePaymentDTO dto);

    /**
     * 处理支付回调
     *
     * @param params 回调参数
     */
    void handleNotify(Map<String, String> params);

    /**
     * 查询支付状态
     *
     * @param outTradeNo 商户订单号
     * @return 支付VO
     */
    PaymentVO queryPaymentStatus(String outTradeNo);
}

