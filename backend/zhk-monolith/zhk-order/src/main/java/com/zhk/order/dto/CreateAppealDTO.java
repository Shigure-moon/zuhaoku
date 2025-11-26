package com.zhk.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建申诉请求 DTO
 *
 * @author shigure
 */
@Data
public class CreateAppealDTO {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "申诉类型不能为空")
    private Integer type; // 1-账号异常, 2-押金争议, 3-其他

    private List<String> evidenceUrls;
}

