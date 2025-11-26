package com.zhk.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 账号密码加密服务（订单模块）
 * 使用 AES-256-GCM 加密算法
 * 注意：与 zhk-user 模块的 EncryptionService 使用相同的加密逻辑
 *
 * @author shigure
 */
@Slf4j
@Service("orderEncryptionService")
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    @Value("${zhk.encryption.master-key:ZHK-RentalCore-2024-MasterKey-32Bytes!!}")
    private String masterKey;

    /**
     * 解密账号密码
     *
     * @param ciphertext 密文（Base64编码）
     * @param accountId  账号ID（用于派生密钥）
     * @return 解密后的明文
     */
    public String decrypt(String ciphertext, Long accountId) {
        try {
            // 生成独立密钥
            SecretKeySpec key = generateKey(accountId);

            // Base64 解码
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            // 提取 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // 提取密文
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // 执行解密
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败: accountId={}, error={}", accountId, e.getMessage(), e);
            // 兼容旧数据：如果是Base64编码的明文，尝试直接解码
            try {
                return new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                throw new RuntimeException("解密失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 为每个账号生成独立密钥
     */
    private SecretKeySpec generateKey(Long accountId) {
        try {
            String keyMaterial = masterKey + ":" + accountId;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            log.error("密钥生成失败: accountId={}, error={}", accountId, e.getMessage(), e);
            throw new RuntimeException("密钥生成失败", e);
        }
    }
}

