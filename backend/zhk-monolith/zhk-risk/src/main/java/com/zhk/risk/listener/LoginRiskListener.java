package com.zhk.risk.listener;

import com.zhk.risk.event.LoginSuccessEvent;
import com.zhk.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 登录风控监听器
 * 监听登录成功事件，执行风控检查
 *
 * @author shigure
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRiskListener {

    private final RiskService riskService;

    /**
     * 监听登录成功事件，执行风控检查
     */
    @Async
    @EventListener
    public void handleLoginSuccess(LoginSuccessEvent event) {
        try {
            log.info("收到登录成功事件，开始风控检查: userId={}, ip={}", event.getUserId(), event.getIpAddress());
            
            if (event.getIpAddress() != null) {
                var riskCheck = riskService.checkLoginRisk(
                        event.getUserId(),
                        event.getIpAddress(),
                        event.getUserAgent(),
                        event.getDeviceFingerprint()
                );
                
                log.info("风控检查完成: userId={}, passed={}, riskLevel={}, needFaceVerification={}",
                        event.getUserId(), riskCheck.getPassed(), riskCheck.getRiskLevel(),
                        riskCheck.getNeedFaceVerification());
            }
        } catch (Exception e) {
            log.error("风控检查失败: userId={}", event.getUserId(), e);
            // 不抛出异常，避免影响登录流程
        }
    }
}

