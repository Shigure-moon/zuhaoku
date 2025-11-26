package com.zhk.risk.service;

import com.zhk.risk.dto.RiskCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 风控服务
 * 整合LocationService和BehaviorService，提供统一的风控检查接口
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final LocationService locationService;
    private final BehaviorService behaviorService;
    
    // 使用 ApplicationContext 来获取 UserMapper，避免直接依赖
    @Autowired(required = false)
    private ApplicationContext applicationContext;

    /**
     * 登录风控检查
     *
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param deviceFingerprint 设备指纹
     * @return 风控检查结果
     */
    @Transactional
    public RiskCheckResult checkLoginRisk(Long userId, String ipAddress, String userAgent, String deviceFingerprint) {
        log.info("开始登录风控检查: userId={}, ip={}", userId, ipAddress);

        // 检查黑名单
        if (behaviorService.isInBlacklist("IP", ipAddress)) {
            RiskCheckResult result = new RiskCheckResult();
            result.setPassed(false);
            result.setRiskLevel(3);
            result.setNeedFreezeAccount(true);
            result.setReason("IP地址在黑名单中");
            log.warn("IP地址在黑名单中: userId={}, ip={}", userId, ipAddress);
            return result;
        }

        if (deviceFingerprint != null && behaviorService.isInBlacklist("DEVICE", deviceFingerprint)) {
            RiskCheckResult result = new RiskCheckResult();
            result.setPassed(false);
            result.setRiskLevel(3);
            result.setNeedFreezeAccount(true);
            result.setReason("设备在黑名单中");
            log.warn("设备在黑名单中: userId={}, device={}", userId, deviceFingerprint);
            return result;
        }

        if (behaviorService.isInBlacklist("USER", String.valueOf(userId))) {
            RiskCheckResult result = new RiskCheckResult();
            result.setPassed(false);
            result.setRiskLevel(3);
            result.setNeedFreezeAccount(true);
            result.setReason("用户账号在黑名单中");
            log.warn("用户账号在黑名单中: userId={}", userId);
            return result;
        }

        // 记录登录并检查异地登录
        var locationInfo = locationService.getLocationByIp(ipAddress);
        RiskCheckResult locationCheck = locationService.checkRemoteLogin(userId, locationInfo);
        
        // 记录登录
        locationService.recordLogin(userId, ipAddress, userAgent, deviceFingerprint);

        // 如果检测到多地登录，冻结账号
        if (locationCheck.getNeedFreezeAccount() != null && locationCheck.getNeedFreezeAccount()) {
            freezeUserAccount(userId, "检测到多地同时登录");
        }

        // 检查异常行为
        boolean hasFrequentCancel = behaviorService.checkFrequentCancel(userId);
        boolean hasPaymentFailure = behaviorService.checkPaymentFailureRate(userId);

        if (hasFrequentCancel || hasPaymentFailure) {
            locationCheck.setPassed(false);
            if (locationCheck.getRiskLevel() < 2) {
                locationCheck.setRiskLevel(2);
            }
            locationCheck.setReason("检测到异常行为：" + 
                    (hasFrequentCancel ? "频繁取消订单" : "") +
                    (hasPaymentFailure ? "支付失败率异常" : ""));
        }

        return locationCheck;
    }

    /**
     * 冻结用户账号
     *
     * @param userId 用户ID
     * @param reason 冻结原因
     */
    private void freezeUserAccount(Long userId, String reason) {
        try {
            // 使用反射获取 UserMapper，避免直接依赖 zhk-user 模块
            if (applicationContext != null) {
                Object userMapper = applicationContext.getBean("userMapper");
                if (userMapper != null) {
                    // 使用反射调用 selectById 和 updateById
                    java.lang.reflect.Method selectById = userMapper.getClass()
                            .getMethod("selectById", Object.class);
                    Object user = selectById.invoke(userMapper, userId);
                    
                    if (user != null) {
                        // 获取 status 字段
                        java.lang.reflect.Method getStatus = user.getClass().getMethod("getStatus");
                        Integer status = (Integer) getStatus.invoke(user);
                        
                        if (status != null && status == 1) {
                            // 设置 status 为 2（冻结）
                            java.lang.reflect.Method setStatus = user.getClass()
                                    .getMethod("setStatus", Integer.class);
                            setStatus.invoke(user, 2);
                            
                            // 更新用户
                            java.lang.reflect.Method updateById = userMapper.getClass()
                                    .getMethod("updateById", Object.class);
                            updateById.invoke(userMapper, user);
                            
                            log.warn("冻结用户账号: userId={}, reason={}", userId, reason);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("冻结用户账号失败: userId={}, reason={}", userId, reason, e);
            // 冻结失败不影响风控检查流程
        }
    }
}

