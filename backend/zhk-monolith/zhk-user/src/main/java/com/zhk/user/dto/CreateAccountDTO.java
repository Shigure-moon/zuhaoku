package com.zhk.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建账号请求 DTO
 *
 * @author shigure
 */
@Data
public class CreateAccountDTO {
    @NotNull(message = "游戏ID不能为空")
    private Integer gameId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "时租价格不能为空")
    @Positive(message = "时租价格必须大于0")
    private BigDecimal pricePerHour;

    private BigDecimal pricePerNight;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Integer level;
}

