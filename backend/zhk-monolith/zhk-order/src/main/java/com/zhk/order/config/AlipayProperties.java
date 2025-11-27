package com.zhk.order.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性
 *
 * @author shigure
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "zhk.alipay")
public class AlipayProperties {
    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用私钥（PKCS#8格式）
     */
    private String privateKey;

    /**
     * 支付宝公钥（从开放平台下载）
     */
    private String alipayPublicKey;

    /**
     * 网关地址
     */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 接口内容加密密钥（AES）
     */
    private String encryptKey;

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 同步跳转地址
     */
    private String returnUrl;

    /**
     * 签名算法
     */
    private String signType = "RSA2";

    /**
     * 字符编码
     */
    private String charset = "UTF-8";
    
    /**
     * 配置加载后验证
     */
    @PostConstruct
    public void validate() {
        log.info("支付宝配置加载验证: appId={}, gatewayUrl={}", appId, gatewayUrl);
        
        if (privateKey != null && !privateKey.isEmpty()) {
            log.debug("私钥长度: {}, 前50字符: {}", 
                    privateKey.length(), 
                    privateKey.length() > 50 ? privateKey.substring(0, 50) + "..." : privateKey);
            
            // 验证私钥格式
            if (!privateKey.contains("BEGIN PRIVATE KEY")) {
                log.warn("⚠️ 私钥格式可能错误！私钥内容前100字符: {}", 
                        privateKey.length() > 100 ? privateKey.substring(0, 100) : privateKey);
            } else {
                log.info("✅ 私钥格式验证通过");
            }
        } else {
            log.warn("⚠️ 私钥未配置（支付宝支付功能将不可用）");
        }
        
        if (alipayPublicKey != null && !alipayPublicKey.isEmpty()) {
            log.debug("公钥长度: {}, 前50字符: {}", 
                    alipayPublicKey.length(), 
                    alipayPublicKey.length() > 50 ? alipayPublicKey.substring(0, 50) + "..." : alipayPublicKey);
            
            if (!alipayPublicKey.contains("BEGIN PUBLIC KEY")) {
                log.warn("⚠️ 公钥格式可能错误！公钥内容前100字符: {}", 
                        alipayPublicKey.length() > 100 ? alipayPublicKey.substring(0, 100) : alipayPublicKey);
            } else {
                log.info("✅ 公钥格式验证通过");
            }
        } else {
            log.warn("⚠️ 公钥未配置（支付宝支付功能将不可用）");
        }
    }
}

