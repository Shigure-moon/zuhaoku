package com.zhk.order.service;

import com.zhk.order.dto.CreateOrderDTO;
import com.zhk.order.dto.OrderVO;
import com.zhk.order.dto.RenewOrderDTO;

import java.util.List;

/**
 * 订单服务接口
 *
 * @author shigure
 */
public interface OrderService {
    /**
     * 创建订单
     */
    OrderVO createOrder(Long userId, CreateOrderDTO dto);

    /**
     * 查询订单列表
     */
    List<OrderVO> getOrderList(Long userId, String role, String status, Integer page, Integer pageSize);

    /**
     * 获取订单总数
     */
    Long getOrderCount(Long userId, String role, String status);

    /**
     * 获取订单详情
     */
    OrderVO getOrderDetail(Long orderId, Long userId);

    /**
     * 续租
     */
    OrderVO renewOrder(Long orderId, Long userId, RenewOrderDTO dto);

    /**
     * 还号
     */
    OrderVO returnAccount(Long orderId, Long userId);

    /**
     * 取消订单
     */
    OrderVO cancelOrder(Long orderId, Long userId);
}

