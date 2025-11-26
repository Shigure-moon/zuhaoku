package com.zhk.risk.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 地理位置信息DTO
 *
 * @author shigure
 */
@Data
public class LocationInfo {
    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;
}

