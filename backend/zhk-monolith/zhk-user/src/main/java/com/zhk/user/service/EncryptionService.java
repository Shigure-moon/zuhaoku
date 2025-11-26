package com.zhk.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 账号密码加密服务
 * 使用 AES-256-GCM 加密算法
 *
 * @author shigure
 */
@Slf4j
@Service("userEncryptionService")
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM 推荐使用 12 字节 IV
    private static final int GCM_TAG_LENGTH = 16; // GCM 认证标签长度

    @Value("${zhk.encryption.master-key:ZHK-RentalCore-2024-MasterKey-32Bytes!!}")
    private String masterKey;

    /**
     * 加密账号密码
     *
     * @param plaintext 明文
     * @param accountId 账号ID（用于派生密钥）
     * @return 加密后的密文（Base64编码）
     */
    public String encrypt(String plaintext, Long accountId) {
        try {
            // 生成独立密钥（基于主密钥和账号ID）
            SecretKey key = generateKey(accountId);

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // 执行加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 将 IV 和密文组合：IV (12字节) + 密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Base64 编码返回
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("加密失败: accountId={}, error={}", accountId, e.getMessage(), e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

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
            SecretKey key = generateKey(accountId);

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
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成初始化向量（IV）
     * 用于兼容旧数据，新数据在加密时自动生成
     *
     * @return Base64 编码的 IV
     */
    public String generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * 为每个账号生成独立密钥
     * 使用主密钥和账号ID派生密钥（简化实现，生产环境应使用 HKDF）
     *
     * @param accountId 账号ID
     * @return 密钥
     */
    private SecretKey generateKey(Long accountId) {
        try {
            // 使用主密钥和账号ID派生密钥
            // 注意：这是简化实现，生产环境应使用 HKDF (HMAC-based Key Derivation Function)
            String keyMaterial = masterKey + ":" + accountId;

            // 使用 SHA-256 哈希确保密钥长度为 32 字节（256 位）
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));

            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            log.error("密钥生成失败: accountId={}, error={}", accountId, e.getMessage(), e);
            throw new RuntimeException("密钥生成失败", e);
        }
    }
}

