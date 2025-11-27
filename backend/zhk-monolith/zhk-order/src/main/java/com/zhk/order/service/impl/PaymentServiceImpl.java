package com.zhk.order.service.impl;

import com.zhk.common.web.BusinessException;
import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.entity.PaymentRecord;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.order.mapper.PaymentRecordMapper;
import com.zhk.order.service.AlipayPaymentService;
import com.zhk.order.service.PaymentService;
import com.zhk.order.entity.Account;
import com.zhk.order.mapper.OrderAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentMapper;
    private final LeaseOrderMapper orderMapper;
    private final OrderAccountMapper accountMapper;
    private final AlipayPaymentService alipayPaymentService;

    public PaymentServiceImpl(
            PaymentRecordMapper paymentMapper,
            LeaseOrderMapper orderMapper,
            OrderAccountMapper accountMapper,
            @Lazy AlipayPaymentService alipayPaymentService) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.accountMapper = accountMapper;
        this.alipayPaymentService = alipayPaymentService;
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

        // 检查是否已有支付记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getOrderId, dto.getOrderId())
               .eq(PaymentRecord::getStatus, "pending");
        PaymentRecord existingPayment = paymentMapper.selectOne(wrapper);
        
        if (existingPayment != null) {
            // 返回已有支付记录，确保包含支付URL
            PaymentVO vo = convertToVO(existingPayment);
            if (vo.getPaymentUrl() == null || vo.getPaymentUrl().isEmpty()) {
                vo.setPaymentUrl("/pay/" + existingPayment.getId());
            }
            return vo;
        }

        // 创建支付记录
        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(dto.getOrderId());
        payment.setPaymentType(dto.getPaymentType());
        payment.setAmount(order.getAmount().add(order.getDeposit()));
        payment.setStatus("pending");
        payment.setTransactionId(UUID.randomUUID().toString().replace("-", ""));

        paymentMapper.insert(payment);

        // 根据支付方式调用不同的支付服务
        if ("alipay".equals(dto.getPaymentType())) {
            // 调用支付宝支付服务
            PaymentVO result = alipayPaymentService.createPayment(userId, dto);
            // 验证返回结果
            if (result == null) {
                throw new BusinessException(500, "支付宝支付创建失败：返回结果为空");
            }
            if (result.getPaymentUrl() == null || result.getPaymentUrl().isEmpty()) {
                log.error("支付宝支付URL为空: paymentId={}", payment.getId());
                throw new BusinessException(500, "支付宝支付URL生成失败");
            }
            // 验证支付URL是否是真实的支付宝URL
            if (result.getPaymentUrl().startsWith("/pay/")) {
                log.warn("支付宝返回了测试支付页面URL，可能是配置问题: paymentId={}", payment.getId());
                throw new BusinessException(500, "支付宝支付配置错误，返回了测试支付页面");
            }
            log.info("支付宝支付创建成功: paymentId={}, paymentUrl长度={}", 
                    payment.getId(), result.getPaymentUrl().length());
            return result;
        } else if ("wechat".equals(dto.getPaymentType())) {
            // TODO: 调用微信支付服务
            PaymentVO vo = convertToVO(payment);
            vo.setPaymentUrl("/pay/" + payment.getId());
            vo.setQrCode("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
            return vo;
        } else {
            throw new BusinessException(400, "不支持的支付方式: " + dto.getPaymentType());
        }
    }

    @Override
    public PaymentVO getPaymentStatus(Long paymentId, Long userId) {
        PaymentRecord payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException(404, "支付记录不存在");
        }

        // 验证权限
        LeaseOrder order = orderMapper.selectById(payment.getOrderId());
        if (order == null || !order.getTenantUid().equals(userId)) {
            throw new BusinessException(403, "无权限查看此支付记录");
        }

        return convertToVO(payment);
    }

    @Override
    @Transactional
    public void onPaymentSuccess(Long paymentId) {
        PaymentRecord payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException(404, "支付记录不存在");
        }

        // 更新支付状态
        payment.setStatus("success");
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 更新订单状态
        LeaseOrder order = orderMapper.selectById(payment.getOrderId());
        if (order != null && "paying".equals(order.getStatus())) {
            order.setStatus("leasing");
            orderMapper.updateById(order);

            // 更新账号状态为租赁中
            Account account = accountMapper.selectById(order.getAccountId());
            if (account != null) {
                account.setStatus(3); // 租赁中
                accountMapper.updateById(account);
            }
        }
    }

    /**
     * 转换为 VO
     */
    private PaymentVO convertToVO(PaymentRecord payment) {
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        // 如果支付状态是 pending 且没有支付URL，设置测试支付页面URL
        if (payment != null && "pending".equals(payment.getStatus()) 
            && (vo.getPaymentUrl() == null || vo.getPaymentUrl().isEmpty())) {
            vo.setPaymentUrl("/pay/" + payment.getId());
        }
        return vo;
    }
}

