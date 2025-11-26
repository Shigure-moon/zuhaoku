package com.zhk.order.controller;

import com.zhk.order.service.AlipayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调控制器
 *
 * @author shigure
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/alipay")
@RequiredArgsConstructor
public class AlipayNotifyController {

    private final AlipayPaymentService alipayPaymentService;

    /**
     * 支付宝异步通知
     * 注意：此接口需要配置在支付宝开放平台的应用网关中
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();

            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }

            log.info("收到支付宝异步通知: {}", params);
            alipayPaymentService.handleNotify(params);

            // 必须返回 "success"，否则支付宝会重复通知
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return "fail";
        }
    }

    /**
     * 支付宝同步跳转
     * 用户支付完成后，支付宝会跳转到此地址
     */
    @GetMapping("/return")
    public String returnUrl(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();

            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }

            log.info("收到支付宝同步跳转: {}", params);
            
            // 验证签名并处理
            alipayPaymentService.handleNotify(params);

            // 重定向到前端订单页面
            return "redirect:http://localhost:3000/tenant/orders";
        } catch (Exception e) {
            log.error("处理支付宝同步跳转失败", e);
            return "redirect:http://localhost:3000/tenant/orders?error=payment_failed";
        }
    }
}

