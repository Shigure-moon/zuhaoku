package com.zhk.user.controller;

import com.zhk.common.security.SecurityUtils;
import com.zhk.common.web.BusinessException;
import com.zhk.common.web.Result;
import com.zhk.user.dto.LoginDTO;
import com.zhk.user.dto.LoginVO;
import com.zhk.user.dto.RegisterDTO;
import com.zhk.user.dto.UserVO;
import com.zhk.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid RegisterDTO dto) {
        UserVO user = userService.register(dto);
        return Result.success("注册成功", user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(
            @RequestBody @Valid LoginDTO dto,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        // 获取客户端IP和User-Agent
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        LoginVO loginVO = userService.login(dto, ipAddress, userAgent);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理IPv6本地回环地址，转换为IPv4格式
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        // 处理X-Forwarded-For可能包含多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<UserVO> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        UserVO user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        // 检查权限：只能查看自己的信息，或者管理员可以查看所有
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }
        
        // 如果不是查看自己的信息，需要管理员权限
        if (!currentUserId.equals(userId)) {
            String role = SecurityUtils.getCurrentUserRole();
            if (role == null || !"OPERATOR".equals(role)) {
                throw new BusinessException(403, "无权限查看其他用户信息");
            }
        }

        UserVO user = userService.getUserById(userId);
        return Result.success(user);
    }
}

