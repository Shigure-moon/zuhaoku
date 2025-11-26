package com.zhk.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.common.security.SecurityUtils;
import com.zhk.common.web.BusinessException;
import com.zhk.common.web.Result;
import com.zhk.order.entity.Appeal;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.mapper.AppealMapper;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.user.dto.UserVO;
import com.zhk.user.entity.Account;
import com.zhk.user.entity.User;
import com.zhk.user.mapper.AccountMapper;
import com.zhk.user.mapper.UserMapper;
import com.zhk.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final LeaseOrderMapper orderMapper;
    private final AppealMapper appealMapper;

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Map<String, Object>> getStats() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        // 验证角色
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"OPERATOR".equals(currentUser.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }

        Map<String, Object> stats = new HashMap<>();
        
        // 总用户数
        long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);
        
        // 总账号数
        long totalAccounts = accountMapper.selectCount(null);
        stats.put("totalAccounts", totalAccounts);
        
        // 总订单数
        long totalOrders = orderMapper.selectCount(null);
        stats.put("totalOrders", totalOrders);
        
        // 待处理申诉数（verdict为null表示待处理）
        LambdaQueryWrapper<Appeal> appealWrapper = new LambdaQueryWrapper<>();
        appealWrapper.isNull(Appeal::getVerdict);
        long pendingAppeals = appealMapper.selectCount(appealWrapper);
        stats.put("pendingAppeals", pendingAppeals);

        return Result.success(stats);
    }

    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        // 验证角色
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"OPERATOR".equals(currentUser.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (mobile != null && !mobile.isEmpty()) {
            wrapper.like(User::getMobile, mobile);
        }
        
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        Page<User> pageParam = new Page<>(page, pageSize);
        Page<User> userPage = userMapper.selectPage(pageParam, wrapper);
        
        List<UserVO> userList = userPage.getRecords().stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    vo.setUserId(user.getId());
                    vo.setNickname(user.getNickname());
                    vo.setMobile(user.getMobile());
                    vo.setRole(user.getRole());
                    vo.setZhimaScore(user.getZhimaScore());
                    vo.setStatus(user.getStatus());
                    vo.setCreatedAt(user.getCreatedAt());
                    vo.setUpdatedAt(user.getUpdatedAt());
                    return vo;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("total", userPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    /**
     * 更新用户状态（冻结/解冻）
     */
    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<String> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        // 验证角色
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"OPERATOR".equals(currentUser.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        return Result.success(status == 1 ? "解冻成功" : "冻结成功");
    }

    /**
     * 获取最近订单（管理员）
     */
    @GetMapping("/orders/recent")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<List<Map<String, Object>>> getRecentOrders(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        // 验证角色
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"OPERATOR".equals(currentUser.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }

        LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LeaseOrder::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        
        List<LeaseOrder> orders = orderMapper.selectList(wrapper);
        
        List<Map<String, Object>> orderList = orders.stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId());
                    map.put("accountId", order.getAccountId());
                    map.put("status", order.getStatus());
                    map.put("amount", order.getAmount());
                    map.put("createdAt", order.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
        
        return Result.success(orderList);
    }

    /**
     * 获取最近申诉（管理员）
     */
    @GetMapping("/appeals/recent")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<List<Map<String, Object>>> getRecentAppeals(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        // 验证角色
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"OPERATOR".equals(currentUser.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }

        // 查询最近申诉
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Appeal::getCreateTime);
        wrapper.last("LIMIT " + limit);
        
        List<Appeal> appeals = appealMapper.selectList(wrapper);
        
        List<Map<String, Object>> appealList = appeals.stream()
                .map(appeal -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", appeal.getId());
                    map.put("orderId", appeal.getOrderId());
                    map.put("type", appeal.getType());
                    map.put("verdict", appeal.getVerdict());
                    map.put("createTime", appeal.getCreateTime());
                    map.put("resolveTime", appeal.getResolveTime());
                    return map;
                })
                .collect(Collectors.toList());
        
        return Result.success(appealList);
    }
}

