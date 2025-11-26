package com.zhk.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理申诉请求 DTO
 *
 * @author shigure
 */
@Data
public class ResolveAppealDTO {
    @NotNull(message = "裁决结果不能为空")
    private Integer verdict; // 1-支持租客, 2-支持号主, 3-各担一半
}

