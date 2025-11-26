package com.zhk.user.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新账号请求 DTO
 *
 * @author shigure
 */
@Data
public class UpdateAccountDTO {
    private String title;
    private String description;
    
    @Positive(message = "时租价格必须大于0")
    private BigDecimal pricePerHour;
    
    private BigDecimal pricePerNight;
    private Integer level;
}

