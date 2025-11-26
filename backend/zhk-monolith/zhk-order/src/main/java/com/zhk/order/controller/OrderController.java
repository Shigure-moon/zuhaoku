package com.zhk.order.controller;

import com.zhk.common.security.JwtUtil;
import com.zhk.common.web.Result;
import com.zhk.order.dto.CreateOrderDTO;
import com.zhk.order.dto.OrderVO;
import com.zhk.order.dto.RenewOrderDTO;
import com.zhk.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderVO> createOrder(
            HttpServletRequest request,
            @RequestBody @Valid CreateOrderDTO dto
    ) {
        Long userId = getUserIdFromRequest(request);
        OrderVO order = orderService.createOrder(userId, dto);
        return Result.success("订单创建成功", order);
    }

    /**
     * 查询订单列表
     */
    @GetMapping
    public Result<Map<String, Object>> getOrderList(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Long userId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        // 转换前端状态为后端状态
        String backendStatus = status != null ? com.zhk.order.util.OrderStatusConverter.toBackendStatus(status) : null;

        List<OrderVO> list = orderService.getOrderList(userId, role, backendStatus, page, pageSize);
        Long total = orderService.getOrderCount(userId, role, backendStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取我的订单列表（租客端）
     */
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyOrders(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Long userId = getUserIdFromRequest(request);

        // 转换前端状态为后端状态
        String backendStatus = status != null ? com.zhk.order.util.OrderStatusConverter.toBackendStatus(status) : null;

        List<OrderVO> list = orderService.getOrderList(userId, "TENANT", backendStatus, page, pageSize);
        Long total = orderService.getOrderCount(userId, "TENANT", backendStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取商家订单列表（商家端）
     */
    @GetMapping("/owner")
    public Result<Map<String, Object>> getOwnerOrders(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Long userId = getUserIdFromRequest(request);

        // 转换前端状态为后端状态
        String backendStatus = status != null ? com.zhk.order.util.OrderStatusConverter.toBackendStatus(status) : null;

        List<OrderVO> list = orderService.getOrderList(userId, "OWNER", backendStatus, page, pageSize);
        Long total = orderService.getOrderCount(userId, "OWNER", backendStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserIdFromRequest(request);
        OrderVO order = orderService.getOrderDetail(id, userId);
        return Result.success(order);
    }

    /**
     * 续租
     */
    @PostMapping("/{id}/renew")
    public Result<OrderVO> renewOrder(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody @Valid RenewOrderDTO dto
    ) {
        Long userId = getUserIdFromRequest(request);
        OrderVO order = orderService.renewOrder(id, userId, dto);
        return Result.success("续租成功", order);
    }

    /**
     * 还号
     */
    @PostMapping("/{id}/return")
    public Result<OrderVO> returnAccount(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserIdFromRequest(request);
        OrderVO order = orderService.returnAccount(id, userId);
        return Result.success("还号成功", order);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancelOrder(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserIdFromRequest(request);
        OrderVO order = orderService.cancelOrder(id, userId);
        return Result.success("订单已取消", order);
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

