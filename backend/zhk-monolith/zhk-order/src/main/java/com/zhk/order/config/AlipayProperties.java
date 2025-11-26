package com.zhk.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性
 *
 * @author shigure
 */
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
}

