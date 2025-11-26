package com.zhk.user.controller;

import com.zhk.common.security.JwtUtil;
import com.zhk.common.web.Result;
import com.zhk.user.dto.AccountVO;
import com.zhk.user.dto.CreateAccountDTO;
import com.zhk.user.dto.UpdateAccountDTO;
import com.zhk.user.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 账号控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final JwtUtil jwtUtil;

    /**
     * 获取账号列表（公开接口，无需认证）
     */
    @GetMapping
    public Result<Map<String, Object>> getAccountList(
            @RequestParam(value = "gameId", required = false) Integer gameId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "12") Integer pageSize,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortOrder", required = false) String sortOrder
    ) {
        Map<String, Object> result = accountService.getAccountList(
                gameId, keyword, minPrice, maxPrice, status, page, pageSize, sortBy, sortOrder
        );
        return Result.success(result);
    }

    /**
     * 获取我的账号列表（商家端，需要认证）
     */
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyAccounts(
            HttpServletRequest request,
            @RequestParam(value = "gameId", required = false) Integer gameId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        // 从请求头获取 Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未授权，请先登录");
        }

        // 从 Token 中获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return Result.error(401, "Token 无效或已过期");
        }

        Map<String, Object> result = accountService.getMyAccounts(userId, gameId, status, page, pageSize);
        return Result.success(result);
    }

    /**
     * 创建账号（商家端，需要认证）
     */
    @PostMapping
    public Result<AccountVO> createAccount(
            HttpServletRequest request,
            @RequestBody @Valid CreateAccountDTO dto
    ) {
        // 从请求头获取 Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未授权，请先登录");
        }

        // 从 Token 中获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return Result.error(401, "Token 无效或已过期");
        }

        AccountVO vo = accountService.createAccount(userId, dto);
        return Result.success("创建成功", vo);
    }

    /**
     * 获取账号详情
     */
    @GetMapping("/{id}")
    public Result<AccountVO> getAccountDetail(@PathVariable Long id) {
        AccountVO vo = accountService.getAccountDetail(id);
        return Result.success(vo);
    }

    /**
     * 更新账号（商家端，需要认证）
     */
    @PutMapping("/{id}")
    public Result<AccountVO> updateAccount(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody @Valid UpdateAccountDTO dto
    ) {
        // 从请求头获取 Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未授权，请先登录");
        }

        // 从 Token 中获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return Result.error(401, "Token 无效或已过期");
        }

        AccountVO vo = accountService.updateAccount(userId, id, dto);
        return Result.success("更新成功", vo);
    }

    /**
     * 删除账号（商家端，需要认证）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAccount(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        // 从请求头获取 Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未授权，请先登录");
        }

        // 从 Token 中获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return Result.error(401, "Token 无效或已过期");
        }

        accountService.deleteAccount(userId, id);
        return Result.success("删除成功", null);
    }

    /**
     * 上架/下架账号（商家端，需要认证）
     */
    @PatchMapping("/{id}/status")
    public Result<AccountVO> toggleAccountStatus(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody
    ) {
        // 从请求头获取 Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未授权，请先登录");
        }

        // 从 Token 中获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return Result.error(401, "Token 无效或已过期");
        }

        // 获取状态
        String statusStr = requestBody.get("status");
        if (statusStr == null) {
            return Result.error(400, "状态参数不能为空");
        }

        AccountVO vo;
        if ("ONLINE".equals(statusStr)) {
            vo = accountService.onlineAccount(userId, id);
        } else if ("OFFLINE".equals(statusStr)) {
            vo = accountService.offlineAccount(userId, id);
        } else {
            return Result.error(400, "无效的状态值");
        }

        return Result.success("操作成功", vo);
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

