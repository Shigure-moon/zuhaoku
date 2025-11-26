package com.zhk.order.util;

import java.time.LocalDateTime;

/**
 * 订单状态转换工具类
 * 将后端状态（paying, leasing, closed, appeal, cancelled）转换为前端状态
 *
 * @author shigure
 */
public class OrderStatusConverter {

    /**
     * 将后端状态转换为前端状态
     *
     * @param backendStatus 后端状态：paying, leasing, closed, appeal, cancelled
     * @param endTime 订单结束时间（用于判断是否过期）
     * @return 前端状态：PENDING_PAYMENT, PAID, ACTIVE, EXPIRED, RETURNED, CANCELLED, DISPUTED, COMPLETED
     */
    public static String toFrontendStatus(String backendStatus, LocalDateTime endTime) {
        if (backendStatus == null) {
            return "PENDING_PAYMENT";
        }

        switch (backendStatus) {
            case "paying":
                return "PENDING_PAYMENT";
            case "leasing":
                // 检查是否过期
                if (endTime != null && LocalDateTime.now().isAfter(endTime)) {
                    return "EXPIRED";
                }
                return "ACTIVE";
            case "closed":
                return "COMPLETED";
            case "appeal":
                return "DISPUTED";
            case "cancelled":
                return "CANCELLED";
            default:
                return "PENDING_PAYMENT";
        }
    }

    /**
     * 将前端状态转换为后端状态
     *
     * @param frontendStatus 前端状态
     * @return 后端状态
     */
    public static String toBackendStatus(String frontendStatus) {
        if (frontendStatus == null) {
            return "paying";
        }

        switch (frontendStatus) {
            case "PENDING_PAYMENT":
                return "paying";
            case "PAID":
            case "ACTIVE":
                return "leasing";
            case "RETURNED":
            case "COMPLETED":
                return "closed";
            case "DISPUTED":
                return "appeal";
            case "CANCELLED":
                return "cancelled";
            case "EXPIRED":
                return "leasing"; // 过期订单仍保持 leasing 状态，但前端显示为 EXPIRED
            default:
                return "paying";
        }
    }
}

