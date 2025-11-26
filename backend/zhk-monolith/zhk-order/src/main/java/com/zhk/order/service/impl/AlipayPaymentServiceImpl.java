package com.zhk.order.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhk.common.web.BusinessException;
import com.zhk.order.config.AlipayProperties;
import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.entity.PaymentRecord;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.order.mapper.PaymentRecordMapper;
import com.zhk.order.service.AlipayPaymentService;
import com.zhk.order.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付宝支付服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
public class AlipayPaymentServiceImpl implements AlipayPaymentService {

    @Autowired(required = false)
    private AlipayClient alipayClient; // 可能为 null（如果配置不存在）
    
    @Autowired(required = false)
    private AlipayProperties alipayProperties; // 可能为 null（如果配置不存在）
    
    private final PaymentRecordMapper paymentMapper;
    private final LeaseOrderMapper orderMapper;
    private final PaymentService paymentService;

    public AlipayPaymentServiceImpl(
            PaymentRecordMapper paymentMapper,
            LeaseOrderMapper orderMapper,
            PaymentService paymentService) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public PaymentVO createPayment(Long userId, CreatePaymentDTO dto) {
        // 查询订单
        LeaseOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 验证权限
        if (!order.getTenantUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 检查订单状态
        if (!"paying".equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态不正确，无法支付");
        }

        // 查询或创建支付记录
        PaymentRecord payment = getOrCreatePayment(dto.getOrderId(), order);

        try {
            // 检查支付宝客户端是否可用
            if (alipayClient == null) {
                log.warn("支付宝客户端未配置，使用测试支付页面");
                PaymentVO vo = new PaymentVO();
                BeanUtils.copyProperties(payment, vo);
                vo.setPaymentUrl("/pay/" + payment.getId());
                return vo;
            }

            // 检查配置是否可用
            if (alipayProperties == null || alipayProperties.getAppId() == null) {
                log.warn("支付宝配置未完成，使用测试支付页面");
                PaymentVO vo = new PaymentVO();
                BeanUtils.copyProperties(payment, vo);
                vo.setPaymentUrl("/pay/" + payment.getId());
                return vo;
            }

            // 创建支付请求
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

            // 设置业务参数
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(payment.getTransactionId());
            model.setTotalAmount(order.getAmount().add(order.getDeposit()).toString());
            model.setSubject("租号酷-账号租赁");
            model.setBody("订单号: " + order.getId() + ", 账号: " + order.getAccountId());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");

            request.setBizModel(model);
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setReturnUrl(alipayProperties.getReturnUrl());

            // 调用接口
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

            if (response.isSuccess()) {
                PaymentVO vo = new PaymentVO();
                BeanUtils.copyProperties(payment, vo);
                vo.setPaymentUrl(response.getBody()); // 返回支付表单HTML
                return vo;
            } else {
                log.error("支付宝支付创建失败: {}", response.getSubMsg());
                // 如果支付宝调用失败，返回测试支付页面
                PaymentVO vo = new PaymentVO();
                BeanUtils.copyProperties(payment, vo);
                vo.setPaymentUrl("/pay/" + payment.getId());
                return vo;
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝接口失败", e);
            // 如果支付宝调用失败，返回测试支付页面
            PaymentVO vo = new PaymentVO();
            BeanUtils.copyProperties(payment, vo);
            vo.setPaymentUrl("/pay/" + payment.getId());
            return vo;
        } catch (Exception e) {
            log.error("创建支付时发生未知异常", e);
            // 如果发生其他异常，返回测试支付页面
            PaymentVO vo = new PaymentVO();
            BeanUtils.copyProperties(payment, vo);
            vo.setPaymentUrl("/pay/" + payment.getId());
            return vo;
        }
    }

    @Override
    @Transactional
    public void handleNotify(Map<String, String> params) {
        log.info("收到支付宝支付回调: {}", params);

        // 验证签名
        if (!verifySign(params)) {
            log.error("支付宝回调签名验证失败");
            throw new BusinessException(400, "签名验证失败");
        }

        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");

        // 查询支付记录
        PaymentRecord payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getTransactionId, outTradeNo)
        );

        if (payment == null) {
            log.error("支付记录不存在: {}", outTradeNo);
            return;
        }

        // 处理支付结果
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            if ("pending".equals(payment.getStatus())) {
                payment.setStatus("success");
                payment.setPaidAt(LocalDateTime.now());
                paymentMapper.updateById(payment);

                // 更新订单状态
                paymentService.onPaymentSuccess(payment.getId());
                log.info("支付成功: orderId={}, paymentId={}, tradeNo={}", 
                        payment.getOrderId(), payment.getId(), tradeNo);
            }
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            payment.setStatus("failed");
            paymentMapper.updateById(payment);
            log.info("支付关闭: orderId={}, paymentId={}", payment.getOrderId(), payment.getId());
        }
    }

    @Override
    public PaymentVO queryPaymentStatus(String outTradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                PaymentRecord payment = paymentMapper.selectOne(
                        new LambdaQueryWrapper<PaymentRecord>()
                                .eq(PaymentRecord::getTransactionId, outTradeNo)
                );

                if (payment != null) {
                    PaymentVO vo = new PaymentVO();
                    BeanUtils.copyProperties(payment, vo);
                    vo.setStatus(mapTradeStatus(response.getTradeStatus()));
                    return vo;
                }
            }
        } catch (AlipayApiException e) {
            log.error("查询支付状态失败", e);
        }

        return null;
    }

    /**
     * 获取或创建支付记录
     */
    private PaymentRecord getOrCreatePayment(Long orderId, LeaseOrder order) {
        PaymentRecord existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderId)
                        .eq(PaymentRecord::getPaymentType, "alipay")
                        .eq(PaymentRecord::getStatus, "pending")
        );

        if (existing != null) {
            return existing;
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPaymentType("alipay");
        payment.setAmount(order.getAmount().add(order.getDeposit()));
        payment.setStatus("pending");
        payment.setTransactionId("ALIPAY" + System.currentTimeMillis() + orderId);
        paymentMapper.insert(payment);

        return payment;
    }

    /**
     * 验证签名
     */
    private boolean verifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
        } catch (Exception e) {
            log.error("签名验证异常", e);
            return false;
        }
    }

    /**
     * 映射交易状态
     */
    private String mapTradeStatus(String tradeStatus) {
        if (tradeStatus == null) {
            return "pending";
        }
        switch (tradeStatus) {
            case "WAIT_BUYER_PAY":
                return "pending";
            case "TRADE_SUCCESS":
            case "TRADE_FINISHED":
                return "success";
            case "TRADE_CLOSED":
                return "failed";
            default:
                return "pending";
        }
    }
}

