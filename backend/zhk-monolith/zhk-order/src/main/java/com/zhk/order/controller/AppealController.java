package com.zhk.order.controller;

import com.zhk.common.security.JwtUtil;
import com.zhk.common.web.Result;
import com.zhk.order.dto.AppealVO;
import com.zhk.order.dto.CreateAppealDTO;
import com.zhk.order.dto.ResolveAppealDTO;
import com.zhk.order.service.AppealService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 申诉控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final JwtUtil jwtUtil;

    /**
     * 创建申诉
     */
    @PostMapping
    public Result<AppealVO> createAppeal(
            HttpServletRequest request,
            @RequestBody @Valid CreateAppealDTO dto
    ) {
        Long userId = getUserIdFromRequest(request);
        AppealVO appeal = appealService.createAppeal(userId, dto);
        return Result.success("申诉创建成功", appeal);
    }

    /**
     * 查询申诉列表
     */
    @GetMapping
    public Result<Map<String, Object>> getAppealList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        List<AppealVO> list = appealService.getAppealList(status, page, pageSize);
        Long total = appealService.getAppealCount(status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取申诉详情
     */
    @GetMapping("/{id}")
    public Result<AppealVO> getAppealDetail(@PathVariable Long id) {
        AppealVO appeal = appealService.getAppealDetail(id);
        return Result.success(appeal);
    }

    /**
     * 处理申诉（管理员）
     */
    @PostMapping("/{id}/resolve")
    public Result<AppealVO> resolveAppeal(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody @Valid ResolveAppealDTO dto
    ) {
        System.out.println("收到处理申诉请求: appealId=" + id + ", verdict=" + dto.getVerdict());
        Long operatorId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);
        System.out.println("操作人ID: " + operatorId + ", 角色: " + role);
        
        // 验证是否为管理员
        if (!"OPERATOR".equals(role)) {
            System.out.println("权限验证失败: 角色=" + role);
            throw new com.zhk.common.web.BusinessException(403, "无权限处理申诉");
        }

        System.out.println("开始处理申诉: appealId=" + id);
        AppealVO appeal = appealService.resolveAppeal(id, operatorId, dto);
        System.out.println("申诉处理完成: appealId=" + id + ", verdict=" + appeal.getVerdict());
        return Result.success("申诉处理成功", appeal);
    }

    /**
     * 从请求头获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            throw new com.zhk.common.web.BusinessException(401, "未授权，请先登录");
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new com.zhk.common.web.BusinessException(401, "Token 无效或已过期");
        }
    }

    /**
     * 从请求头获取角色
     */
    private String getRoleFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        try {
            return jwtUtil.getRoleFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从请求头获取 Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

