package com.zhk.risk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.common.security.SecurityUtils;
import com.zhk.common.web.BusinessException;
import com.zhk.common.web.Result;
import com.zhk.risk.dto.RiskCheckResult;
import com.zhk.risk.entity.AbnormalBehavior;
import com.zhk.risk.entity.Blacklist;
import com.zhk.risk.entity.UserLoginRecord;
import com.zhk.risk.mapper.AbnormalBehaviorMapper;
import com.zhk.risk.mapper.BlacklistMapper;
import com.zhk.risk.mapper.UserLoginRecordMapper;
import com.zhk.risk.service.BehaviorService;
import com.zhk.risk.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控控制器
 *
 * @author shigure
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
public class RiskController {

    private final UserLoginRecordMapper loginRecordMapper;
    private final AbnormalBehaviorMapper behaviorMapper;
    private final BlacklistMapper blacklistMapper;
    private final LocationService locationService;
    private final BehaviorService behaviorService;

    /**
     * 获取登录记录列表
     */
    @GetMapping("/login-records")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Page<UserLoginRecord>> getLoginRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        log.info("查询登录记录: userId={}, ipAddress={}, riskLevel={}, page={}, pageSize={}", 
                userId, ipAddress, riskLevel, page, pageSize);
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            log.warn("未授权访问登录记录接口");
            throw new BusinessException(401, "未授权，请先登录");
        }

        try {
            LambdaQueryWrapper<UserLoginRecord> wrapper = new LambdaQueryWrapper<>();
            if (userId != null) {
                wrapper.eq(UserLoginRecord::getUserId, userId);
            }
            if (ipAddress != null && !ipAddress.isEmpty()) {
                wrapper.eq(UserLoginRecord::getIpAddress, ipAddress);
            }
            if (riskLevel != null) {
                wrapper.eq(UserLoginRecord::getRiskLevel, riskLevel);
            }
            wrapper.orderByDesc(UserLoginRecord::getLoginTime);

            Page<UserLoginRecord> pageParam = new Page<>(page, pageSize);
            Page<UserLoginRecord> result = loginRecordMapper.selectPage(pageParam, wrapper);
            
            log.info("查询登录记录成功: total={}, size={}", result.getTotal(), result.getSize());
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询登录记录失败", e);
            throw new BusinessException(500, "查询登录记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取异常行为列表
     */
    @GetMapping("/abnormal-behaviors")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Page<AbnormalBehavior>> getAbnormalBehaviors(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String behaviorType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        LambdaQueryWrapper<AbnormalBehavior> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AbnormalBehavior::getUserId, userId);
        }
        if (behaviorType != null && !behaviorType.isEmpty()) {
            wrapper.eq(AbnormalBehavior::getBehaviorType, behaviorType);
        }
        if (status != null) {
            wrapper.eq(AbnormalBehavior::getStatus, status);
        }
        wrapper.orderByDesc(AbnormalBehavior::getCreatedAt);

        Page<AbnormalBehavior> pageParam = new Page<>(page, pageSize);
        try {
            Page<AbnormalBehavior> result = behaviorMapper.selectPage(pageParam, wrapper);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询异常行为失败", e);
            throw new BusinessException(500, "查询异常行为失败: " + e.getMessage());
        }
    }

    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Page<Blacklist>> getBlacklist(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Blacklist::getType, type);
        }
        if (status != null) {
            wrapper.eq(Blacklist::getStatus, status);
        }
        wrapper.orderByDesc(Blacklist::getCreatedAt);

        Page<Blacklist> pageParam = new Page<>(page, pageSize);
        try {
            Page<Blacklist> result = blacklistMapper.selectPage(pageParam, wrapper);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询黑名单失败", e);
            throw new BusinessException(500, "查询黑名单失败: " + e.getMessage());
        }
    }

    /**
     * 添加黑名单
     */
    @PostMapping("/blacklist")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Void> addToBlacklist(
            @RequestBody Map<String, Object> request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        String type = (String) request.get("type");
        String value = (String) request.get("value");
        String reason = (String) request.get("reason");
        Integer riskLevel = request.get("riskLevel") != null ? 
                Integer.valueOf(request.get("riskLevel").toString()) : 2;
        LocalDateTime expiresAt = null;
        if (request.get("expiresAt") != null) {
            expiresAt = LocalDateTime.parse(request.get("expiresAt").toString());
        }

        behaviorService.addToBlacklist(type, value, reason, riskLevel, currentUserId, expiresAt);
        return Result.success("添加成功", null);
    }

    /**
     * 移除黑名单
     */
    @DeleteMapping("/blacklist/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Void> removeFromBlacklist(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        behaviorService.removeFromBlacklist(id);
        return Result.success("移除成功", null);
    }

    /**
     * 处理异常行为
     */
    @PostMapping("/abnormal-behaviors/{id}/handle")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Void> handleAbnormalBehavior(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        AbnormalBehavior behavior = behaviorMapper.selectById(id);
        if (behavior == null) {
            throw new BusinessException(404, "异常行为记录不存在");
        }

        Integer action = request.get("action") != null ? 
                Integer.valueOf(request.get("action").toString()) : 1; // 1-已处理 2-已忽略

        behavior.setStatus(action);
        behavior.setHandledBy(currentUserId);
        behavior.setHandledAt(LocalDateTime.now());
        behaviorMapper.updateById(behavior);

        return Result.success("处理成功", null);
    }

    /**
     * 获取用户风控统计
     */
    @GetMapping("/stats/{userId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Map<String, Object>> getUserRiskStats(@PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        // 查询登录记录数
        LambdaQueryWrapper<UserLoginRecord> loginWrapper = new LambdaQueryWrapper<>();
        loginWrapper.eq(UserLoginRecord::getUserId, userId);
        long loginCount = loginRecordMapper.selectCount(loginWrapper);

        // 查询可疑登录数
        loginWrapper.eq(UserLoginRecord::getIsSuspicious, 1);
        long suspiciousLoginCount = loginRecordMapper.selectCount(loginWrapper);

        // 查询异常行为数
        LambdaQueryWrapper<AbnormalBehavior> behaviorWrapper = new LambdaQueryWrapper<>();
        behaviorWrapper.eq(AbnormalBehavior::getUserId, userId);
        long abnormalBehaviorCount = behaviorMapper.selectCount(behaviorWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("loginCount", loginCount);
        stats.put("suspiciousLoginCount", suspiciousLoginCount);
        stats.put("abnormalBehaviorCount", abnormalBehaviorCount);

        return Result.success("查询成功", stats);
    }
}

