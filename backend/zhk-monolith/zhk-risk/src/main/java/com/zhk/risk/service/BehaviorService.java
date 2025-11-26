package com.zhk.risk.service;

import com.zhk.risk.entity.AbnormalBehavior;
import com.zhk.risk.entity.Blacklist;
import com.zhk.risk.mapper.AbnormalBehaviorMapper;
import com.zhk.risk.mapper.BlacklistMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常行为识别服务
 * 负责检测和记录异常行为
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorService {

    private final AbnormalBehaviorMapper behaviorMapper;
    private final BlacklistMapper blacklistMapper;

    // 频繁取消订单阈值（1小时内）
    private static final int FREQUENT_CANCEL_THRESHOLD = 3;
    // 支付失败率阈值
    private static final double PAYMENT_FAILURE_RATE_THRESHOLD = 0.5;

    /**
     * 检查是否在黑名单中
     *
     * @param type 类型：IP/DEVICE/PHONE/USER
     * @param value 值
     * @return 是否在黑名单中
     */
    public boolean isInBlacklist(String type, String value) {
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Blacklist::getType, type);
        wrapper.eq(Blacklist::getValue, value);
        wrapper.eq(Blacklist::getStatus, 1); // 生效状态
        wrapper.and(w -> w.isNull(Blacklist::getExpiresAt)
                .or(w2 -> w2.ge(Blacklist::getExpiresAt, LocalDateTime.now())));
        
        Blacklist blacklist = blacklistMapper.selectOne(wrapper);
        return blacklist != null;
    }

    /**
     * 检查频繁取消订单
     *
     * @param userId 用户ID
     * @return 是否异常
     */
    public boolean checkFrequentCancel(Long userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        // 查询1小时内的取消订单数（这里简化实现，实际应从订单服务查询）
        // TODO: 从OrderService查询取消订单数
        int cancelCount = 0; // 占位符
        
        if (cancelCount > FREQUENT_CANCEL_THRESHOLD) {
            recordAbnormalBehavior(userId, "FREQUENT_CANCEL",
                    String.format("1小时内取消订单%d次，超过阈值%d次", cancelCount, FREQUENT_CANCEL_THRESHOLD),
                    60, "ORDER", null);
            return true;
        }
        
        return false;
    }

    /**
     * 检查支付失败率
     *
     * @param userId 用户ID
     * @return 是否异常
     */
    public boolean checkPaymentFailureRate(Long userId) {
        // TODO: 从PaymentService查询支付失败率
        // 这里简化实现
        double failureRate = 0.0; // 占位符
        
        if (failureRate > PAYMENT_FAILURE_RATE_THRESHOLD) {
            recordAbnormalBehavior(userId, "PAYMENT_FAILURE",
                    String.format("支付失败率%.2f%%，超过阈值%.2f%%", failureRate * 100, PAYMENT_FAILURE_RATE_THRESHOLD * 100),
                    70, "PAYMENT", null);
            return true;
        }
        
        return false;
    }

    /**
     * 检查账号异常使用
     *
     * @param userId 用户ID
     * @param accountId 账号ID
     * @param description 异常描述
     */
    public void checkAccountAbuse(Long userId, Long accountId, String description) {
        recordAbnormalBehavior(userId, "ACCOUNT_ABUSE", description, 80, "ACCOUNT", accountId);
    }

    /**
     * 记录异常行为
     *
     * @param userId 用户ID
     * @param behaviorType 行为类型
     * @param description 描述
     * @param riskScore 风险评分
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     */
    public void recordAbnormalBehavior(Long userId, String behaviorType, String description,
                                      Integer riskScore, String resourceType, Long resourceId) {
        AbnormalBehavior behavior = new AbnormalBehavior();
        behavior.setUserId(userId);
        behavior.setBehaviorType(behaviorType);
        behavior.setDescription(description);
        behavior.setRiskScore(riskScore);
        behavior.setResourceType(resourceType);
        behavior.setResourceId(resourceId);
        behavior.setStatus(0); // 待处理
        behavior.setCreatedAt(LocalDateTime.now());
        
        behaviorMapper.insert(behavior);
        
        log.warn("记录异常行为: userId={}, type={}, description={}, riskScore={}",
                userId, behaviorType, description, riskScore);
        
        // 如果风险评分高，考虑加入黑名单
        if (riskScore >= 80) {
            // TODO: 自动加入黑名单逻辑（需要管理员审核）
            log.warn("高风险异常行为，建议加入黑名单: userId={}, type={}, riskScore={}",
                    userId, behaviorType, riskScore);
        }
    }

    /**
     * 添加黑名单
     *
     * @param type 类型
     * @param value 值
     * @param reason 原因
     * @param riskLevel 风险等级
     * @param createdBy 创建人ID
     * @param expiresAt 过期时间（null表示永久）
     */
    public void addToBlacklist(String type, String value, String reason,
                               Integer riskLevel, Long createdBy, LocalDateTime expiresAt) {
        Blacklist blacklist = new Blacklist();
        blacklist.setType(type);
        blacklist.setValue(value);
        blacklist.setReason(reason);
        blacklist.setRiskLevel(riskLevel);
        blacklist.setStatus(1); // 生效
        blacklist.setCreatedBy(createdBy);
        blacklist.setCreatedAt(LocalDateTime.now());
        blacklist.setExpiresAt(expiresAt);
        
        blacklistMapper.insert(blacklist);
        
        log.info("添加黑名单: type={}, value={}, reason={}", type, value, reason);
    }

    /**
     * 移除黑名单
     *
     * @param id 黑名单ID
     */
    public void removeFromBlacklist(Long id) {
        Blacklist blacklist = blacklistMapper.selectById(id);
        if (blacklist != null) {
            blacklist.setStatus(0); // 失效
            blacklistMapper.updateById(blacklist);
            log.info("移除黑名单: id={}, type={}, value={}", id, blacklist.getType(), blacklist.getValue());
        }
    }

    /**
     * 获取用户的异常行为记录
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 异常行为列表
     */
    public List<AbnormalBehavior> getUserAbnormalBehaviors(Long userId, Integer limit) {
        LambdaQueryWrapper<AbnormalBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AbnormalBehavior::getUserId, userId);
        wrapper.orderByDesc(AbnormalBehavior::getCreatedAt);
        wrapper.last("LIMIT " + (limit != null ? limit : 10));
        
        return behaviorMapper.selectList(wrapper);
    }
}

