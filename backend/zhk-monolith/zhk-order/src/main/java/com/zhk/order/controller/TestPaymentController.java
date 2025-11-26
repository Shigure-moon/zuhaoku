package com.zhk.order.controller;

import com.zhk.common.web.Result;
import com.zhk.order.entity.PaymentRecord;
import com.zhk.order.mapper.PaymentRecordMapper;
import com.zhk.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 测试支付页面控制器
 * 用于开发环境模拟支付流程
 *
 * @author shigure
 */
@Slf4j
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class TestPaymentController {

    private final PaymentRecordMapper paymentMapper;
    private final PaymentService paymentService;

    /**
     * 显示测试支付页面
     */
    @GetMapping(value = "/{paymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public String showPaymentPage(@PathVariable Long paymentId) {
        PaymentRecord payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            return generateErrorPage("支付记录不存在");
        }

        // 如果已经支付成功，显示成功页面
        if ("success".equals(payment.getStatus())) {
            return generateSuccessPage(payment);
        }

        // 生成支付页面
        return generatePaymentPage(payment);
    }

    /**
     * 模拟支付成功
     */
    @PostMapping("/{paymentId}/success")
    public Result<String> simulatePaymentSuccess(@PathVariable Long paymentId) {
        try {
            log.info("模拟支付成功: paymentId={}", paymentId);
            
            // 调用支付成功回调
            paymentService.onPaymentSuccess(paymentId);
            
            return Result.success("支付成功", "/pay/" + paymentId);
        } catch (Exception e) {
            log.error("模拟支付失败", e);
            return Result.error(500, "支付处理失败: " + e.getMessage());
        }
    }

    /**
     * 生成支付页面 HTML
     */
    private String generatePaymentPage(PaymentRecord payment) {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>测试支付页面 - 租号酷</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                    }
                    .payment-container {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        max-width: 500px;
                        width: 100%%;
                        padding: 40px;
                        text-align: center;
                    }
                    .payment-icon {
                        width: 80px;
                        height: 80px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        border-radius: 50%%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 40px;
                        color: white;
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 24px;
                    }
                    .payment-info {
                        background: #f5f5f5;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 20px 0;
                        text-align: left;
                    }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #e0e0e0;
                    }
                    .info-row:last-child {
                        border-bottom: none;
                    }
                    .info-label {
                        color: #666;
                        font-size: 14px;
                    }
                    .info-value {
                        color: #333;
                        font-weight: 600;
                        font-size: 16px;
                    }
                    .amount {
                        font-size: 32px;
                        color: #667eea;
                        font-weight: bold;
                        margin: 20px 0;
                    }
                    .pay-button {
                        width: 100%%;
                        padding: 16px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 18px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: transform 0.2s, box-shadow 0.2s;
                        margin-top: 20px;
                    }
                    .pay-button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 10px 20px rgba(102, 126, 234, 0.4);
                    }
                    .pay-button:active {
                        transform: translateY(0);
                    }
                    .warning {
                        background: #fff3cd;
                        border: 1px solid #ffc107;
                        border-radius: 8px;
                        padding: 12px;
                        margin-top: 20px;
                        color: #856404;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="payment-container">
                    <div class="payment-icon">💰</div>
                    <h1>测试支付页面</h1>
                    <p style="color: #666; margin-bottom: 20px;">开发环境模拟支付</p>
                    
                    <div class="payment-info">
                        <div class="info-row">
                            <span class="info-label">订单号：</span>
                            <span class="info-value">#%d</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">支付方式：</span>
                            <span class="info-value">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">交易号：</span>
                            <span class="info-value">%s</span>
                        </div>
                    </div>
                    
                    <div class="amount">¥%.2f</div>
                    
                    <button class="pay-button" onclick="payNow()">确认支付</button>
                    
                    <div class="warning">
                        ⚠️ 这是测试支付页面，点击确认支付将直接完成支付，无需真实支付流程
                    </div>
                </div>
                
                <script>
                    function payNow() {
                        const button = document.querySelector('.pay-button');
                        button.disabled = true;
                        button.textContent = '支付中...';
                        
                        // 使用当前页面的 origin 和 pathname 构建完整的 URL
                        const currentUrl = window.location.origin + window.location.pathname;
                        const paymentId = currentUrl.split('/').pop();
                        const successUrl = window.location.origin + '/pay/' + paymentId + '/success';
                        
                        fetch(successUrl, {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            }
                        })
                        .then(response => {
                            if (!response.ok) {
                                throw new Error('HTTP error! status: ' + response.status);
                            }
                            return response.json();
                        })
                        .then(data => {
                            if (data.code === 200) {
                                // 延迟一下再跳转，让用户看到支付成功
                                setTimeout(() => {
                                    // 刷新当前页面以显示支付成功页面
                                    window.location.reload();
                                }, 500);
                            } else {
                                alert('支付失败：' + data.message);
                                button.disabled = false;
                                button.textContent = '确认支付';
                            }
                        })
                        .catch(error => {
                            console.error('支付错误:', error);
                            alert('支付失败，请重试: ' + error.message);
                            button.disabled = false;
                            button.textContent = '确认支付';
                        });
                    }
                </script>
            </body>
            </html>
            """.formatted(
                payment.getOrderId(),
                "alipay".equals(payment.getPaymentType()) ? "支付宝" : "微信支付",
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getId(),
                payment.getId()
            );
    }

    /**
     * 生成支付成功页面 HTML
     */
    private String generateSuccessPage(PaymentRecord payment) {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>支付成功 - 租号酷</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                    }
                    .success-container {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        max-width: 500px;
                        width: 100%%;
                        padding: 40px;
                        text-align: center;
                    }
                    .success-icon {
                        width: 100px;
                        height: 100px;
                        background: #38ef7d;
                        border-radius: 50%%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 60px;
                        color: white;
                        animation: scaleIn 0.5s ease-out;
                    }
                    @keyframes scaleIn {
                        from {
                            transform: scale(0);
                        }
                        to {
                            transform: scale(1);
                        }
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 28px;
                    }
                    .success-message {
                        color: #666;
                        margin-bottom: 30px;
                        font-size: 16px;
                    }
                    .payment-info {
                        background: #f5f5f5;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 20px 0;
                        text-align: left;
                    }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #e0e0e0;
                    }
                    .info-row:last-child {
                        border-bottom: none;
                    }
                    .info-label {
                        color: #666;
                        font-size: 14px;
                    }
                    .info-value {
                        color: #333;
                        font-weight: 600;
                        font-size: 16px;
                    }
                    .back-button {
                        width: 100%%;
                        padding: 16px;
                        background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 18px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: transform 0.2s, box-shadow 0.2s;
                        margin-top: 20px;
                        text-decoration: none;
                        display: block;
                    }
                    .back-button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 10px 20px rgba(56, 239, 125, 0.4);
                    }
                </style>
            </head>
            <body>
                <div class="success-container">
                    <div class="success-icon">✓</div>
                    <h1>支付成功！</h1>
                    <p class="success-message">您的订单已支付成功，可以开始使用账号了</p>
                    
                    <div class="payment-info">
                        <div class="info-row">
                            <span class="info-label">订单号：</span>
                            <span class="info-value">#%d</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">支付金额：</span>
                            <span class="info-value">¥%.2f</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">交易号：</span>
                            <span class="info-value">%s</span>
                        </div>
                    </div>
                    
                    <a href="javascript:void(0)" onclick="goBack()" class="back-button">返回订单列表</a>
                    <script>
                        function goBack() {
                            // 尝试关闭窗口（如果是弹窗）
                            if (window.opener) {
                                window.opener.postMessage({type: 'payment_success', orderId: %d}, '*');
                                window.close();
                            } else {
                                // 否则跳转到前端订单列表
                                window.location.href = 'http://localhost:3001/tenant/orders';
                            }
                        }
                        // 监听来自父窗口的消息
                        window.addEventListener('message', function(event) {
                            if (event.data && event.data.type === 'close_payment') {
                                window.close();
                            }
                        });
                    </script>
                </div>
            </body>
            </html>
            """.formatted(
                payment.getOrderId(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getOrderId()
            );
    }

    /**
     * 生成错误页面 HTML
     */
    private String generateErrorPage(String error) {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>支付错误 - 租号酷</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                    }
                    .error-container {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        max-width: 500px;
                        width: 100%%;
                        padding: 40px;
                        text-align: center;
                    }
                    .error-icon {
                        width: 100px;
                        height: 100px;
                        background: #f5576c;
                        border-radius: 50%%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 60px;
                        color: white;
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 28px;
                    }
                    .error-message {
                        color: #666;
                        margin-bottom: 30px;
                        font-size: 16px;
                    }
                    .back-button {
                        width: 100%%;
                        padding: 16px;
                        background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 18px;
                        font-weight: 600;
                        cursor: pointer;
                        text-decoration: none;
                        display: block;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">✗</div>
                    <h1>支付错误</h1>
                    <p class="error-message">%s</p>
                    <a href="/" class="back-button">返回首页</a>
                </div>
            </body>
            </html>
            """.formatted(error);
    }
}

