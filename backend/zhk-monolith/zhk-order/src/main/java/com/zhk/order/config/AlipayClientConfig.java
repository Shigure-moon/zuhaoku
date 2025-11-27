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
@ConditionalOnProperty(prefix = "zhk.alipay", name = "app-id")
public class AlipayClientConfig {

    private final AlipayProperties alipayProperties;

    @Bean
    public AlipayClient alipayClient() {
        // 验证配置
        if (alipayProperties.getAppId() == null || alipayProperties.getAppId().isEmpty()) {
            log.error("支付宝 AppID 未配置！请检查配置：zhk.alipay.app-id");
            throw new IllegalStateException("支付宝 AppID 未配置");
        }
        
        if (alipayProperties.getPrivateKey() == null || alipayProperties.getPrivateKey().isEmpty()) {
            log.error("支付宝私钥未配置！请检查配置：zhk.alipay.private-key");
            throw new IllegalStateException("支付宝私钥未配置");
        }
        
        if (alipayProperties.getAlipayPublicKey() == null || alipayProperties.getAlipayPublicKey().isEmpty()) {
            log.error("支付宝公钥未配置！请检查配置：zhk.alipay.alipay-public-key");
            throw new IllegalStateException("支付宝公钥未配置");
        }
        
        log.info("支付宝客户端配置验证通过: appId={}, gatewayUrl={}, signType={}", 
                alipayProperties.getAppId(), 
                alipayProperties.getGatewayUrl(),
                alipayProperties.getSignType());
        
        AlipayClient client = new DefaultAlipayClient(
                alipayProperties.getGatewayUrl(),
                alipayProperties.getAppId(),
                alipayProperties.getPrivateKey(),
                "json",
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType()
        );

        log.info("✅ 支付宝客户端初始化成功: appId={}, gatewayUrl={}", 
                alipayProperties.getAppId(), alipayProperties.getGatewayUrl());
        
        return client;
    }
}

