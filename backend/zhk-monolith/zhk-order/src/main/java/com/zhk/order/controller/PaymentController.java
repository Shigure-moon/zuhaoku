package com.zhk.order.controller;

import com.zhk.common.security.JwtUtil;
import com.zhk.common.web.Result;
import com.zhk.order.dto.CreatePaymentDTO;
import com.zhk.order.dto.PaymentVO;
import com.zhk.order.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    /**
     * 创建支付
     */
    @PostMapping
    public Result<PaymentVO> createPayment(
            HttpServletRequest request,
            @RequestBody @Valid CreatePaymentDTO dto
    ) {
        Long userId = getUserIdFromRequest(request);
        PaymentVO payment = paymentService.createPayment(userId, dto);
        return Result.success("支付创建成功", payment);
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/{id}/status")
    public Result<PaymentVO> getPaymentStatus(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserIdFromRequest(request);
        PaymentVO payment = paymentService.getPaymentStatus(id, userId);
        return Result.success(payment);
    }

    /**
     * 从请求头获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            throw new com.zhk.common.web.BusinessException(401, "未授权，请先登录");
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new com.zhk.common.web.BusinessException(401, "Token 无效或已过期");
        }
    }

    /**
     * 从请求头获取 Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

