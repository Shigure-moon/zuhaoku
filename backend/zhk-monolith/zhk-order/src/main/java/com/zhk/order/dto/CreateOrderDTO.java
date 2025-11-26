package com.zhk.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 创建订单请求 DTO
 *
 * @author shigure
 */
@Data
public class CreateOrderDTO {
    @NotNull(message = "账号ID不能为空")
    private Long accountId;

    @NotNull(message = "租期时长不能为空")
    @Positive(message = "租期时长必须大于0")
    private Integer duration;

    @NotNull(message = "租期类型不能为空")
    private String durationType; // MINUTE, HOUR, OVERNIGHT
}

