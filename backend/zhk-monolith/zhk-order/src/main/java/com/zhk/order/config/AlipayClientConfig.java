package com.zhk.order.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝客户端配置
 *
 * @author shigure
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
// 移除 @ConditionalOnProperty，允许配置类始终创建，但客户端可能为 null
public class AlipayClientConfig {

    private final AlipayProperties alipayProperties;

    @Bean
    public AlipayClient alipayClient() {
        // 验证配置 - 如果配置不完整，返回 null（不创建客户端）
        if (alipayProperties.getAppId() == null || alipayProperties.getAppId().isEmpty()) {
            log.warn("支付宝 AppID 未配置，支付宝支付功能将不可用。请检查配置：zhk.alipay.app-id");
            return null;
        }
        
        if (alipayProperties.getPrivateKey() == null || alipayProperties.getPrivateKey().isEmpty()) {
            log.warn("支付宝私钥未配置，支付宝支付功能将不可用。请检查配置：zhk.alipay.private-key");
            return null;
        }
        
        // 验证私钥格式
        String privateKeyRaw = alipayProperties.getPrivateKey();
        if (!privateKeyRaw.contains("BEGIN PRIVATE KEY") || !privateKeyRaw.contains("END PRIVATE KEY")) {
            log.warn("支付宝私钥格式错误，支付宝支付功能将不可用。私钥必须包含 BEGIN PRIVATE KEY 和 END PRIVATE KEY 标记");
            log.warn("私钥内容预览（前100字符）: {}", 
                    privateKeyRaw.length() > 100 ? privateKeyRaw.substring(0, 100) + "..." : privateKeyRaw);
            return null;
        }
        
        if (alipayProperties.getAlipayPublicKey() == null || alipayProperties.getAlipayPublicKey().isEmpty()) {
            log.warn("支付宝公钥未配置，支付宝支付功能将不可用。请检查配置：zhk.alipay.alipay-public-key");
            return null;
        }
        
        // 验证公钥格式
        String publicKeyRaw = alipayProperties.getAlipayPublicKey();
        if (!publicKeyRaw.contains("BEGIN PUBLIC KEY") || !publicKeyRaw.contains("END PUBLIC KEY")) {
            log.warn("支付宝公钥格式错误，支付宝支付功能将不可用。公钥必须包含 BEGIN PUBLIC KEY 和 END PUBLIC KEY 标记");
            return null;
        }
        
        // 清理私钥格式（去除可能的额外空格和换行）
        String privateKey = cleanPrivateKey(alipayProperties.getPrivateKey());
        String alipayPublicKey = cleanPublicKey(alipayProperties.getAlipayPublicKey());
        
        // 验证清理后的私钥
        if (privateKey == null || privateKey.isEmpty()) {
            log.error("清理后的私钥为空！");
            throw new IllegalStateException("私钥清理失败");
        }
        
        if (!privateKey.contains("BEGIN PRIVATE KEY") || !privateKey.contains("END PRIVATE KEY")) {
            log.error("清理后的私钥格式仍然错误！");
            log.error("私钥前200字符: {}", privateKey.length() > 200 ? privateKey.substring(0, 200) : privateKey);
            throw new IllegalStateException("私钥格式错误");
        }
        
        log.info("支付宝客户端配置验证通过: appId={}, gatewayUrl={}, signType={}", 
                alipayProperties.getAppId(), 
                alipayProperties.getGatewayUrl(),
                alipayProperties.getSignType());
        
        // 详细验证私钥内容
        log.info("私钥验证: 长度={}, 包含BEGIN={}, 包含END={}", 
                privateKey.length(),
                privateKey.contains("BEGIN PRIVATE KEY"),
                privateKey.contains("END PRIVATE KEY"));
        log.info("私钥前80字符: {}", privateKey.substring(0, Math.min(80, privateKey.length())));
        log.info("私钥后80字符: {}", privateKey.length() > 80 ? 
                "..." + privateKey.substring(privateKey.length() - 80) : privateKey);
        
        // 验证私钥不是请求参数（关键检查）
        if (privateKey.contains("alipay_sdk=") || privateKey.contains("app_id=") || privateKey.contains("biz_content=")) {
            log.error("❌ 严重错误：私钥内容被替换成了请求参数！");
            log.error("私钥实际内容: {}", privateKey);
            throw new IllegalStateException("私钥配置错误：私钥内容被替换成了请求参数，请检查配置文件");
        }
        
        log.info("公钥长度: {}", alipayPublicKey.length());
        
        try {
            log.info("准备创建支付宝客户端，使用私钥长度: {}", privateKey.length());
            AlipayClient client = new DefaultAlipayClient(
                    alipayProperties.getGatewayUrl(),
                    alipayProperties.getAppId(),
                    privateKey,
                    "json",
                    alipayProperties.getCharset(),
                    alipayPublicKey,
                    alipayProperties.getSignType()
            );
            
            log.info("✅ 支付宝客户端初始化成功: appId={}, gatewayUrl={}", 
                    alipayProperties.getAppId(), alipayProperties.getGatewayUrl());
            
            return client;
        } catch (Exception e) {
            log.error("创建支付宝客户端失败，支付宝支付功能将不可用", e);
            log.error("使用的私钥前100字符: {}", privateKey.substring(0, Math.min(100, privateKey.length())));
            // 返回 null 而不是抛出异常，允许应用在没有支付宝的情况下启动
            return null;
        }
    }
    
    /**
     * 清理私钥格式
     * 确保私钥格式正确，去除多余的空格和换行
     */
    private String cleanPrivateKey(String privateKey) {
        if (privateKey == null) {
            return null;
        }
        // 去除首尾空白
        privateKey = privateKey.trim();
        // 如果已经包含 BEGIN/END 标记，直接返回
        if (privateKey.contains("BEGIN PRIVATE KEY") && privateKey.contains("END PRIVATE KEY")) {
            return privateKey;
        }
        // 如果没有标记，添加标记（这种情况不应该发生，但为了安全）
        if (!privateKey.startsWith("-----BEGIN")) {
            return "-----BEGIN PRIVATE KEY-----\n" + privateKey + "\n-----END PRIVATE KEY-----";
        }
        return privateKey;
    }
    
    /**
     * 清理公钥格式
     */
    private String cleanPublicKey(String publicKey) {
        if (publicKey == null) {
            return null;
        }
        // 去除首尾空白
        publicKey = publicKey.trim();
        // 如果已经包含 BEGIN/END 标记，直接返回
        if (publicKey.contains("BEGIN PUBLIC KEY") && publicKey.contains("END PUBLIC KEY")) {
            return publicKey;
        }
        // 如果没有标记，添加标记
        if (!publicKey.startsWith("-----BEGIN")) {
            return "-----BEGIN PUBLIC KEY-----\n" + publicKey + "\n-----END PUBLIC KEY-----";
        }
        return publicKey;
    }
}

