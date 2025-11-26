package com.zhk.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账号展示 VO（不包含敏感信息）
 *
 * @author shigure
 */
@Data
public class AccountVO {
    private Long id;
    private Integer gameId;
    private String gameName;
    private Long ownerId;
    private String ownerNickname;
    private String title; // 账号标题（可以从其他字段组合或单独存储）
    private String description; // 账号描述
    private BigDecimal pricePerHour; // 时租价格（从 price1h 计算）
    private BigDecimal pricePerDay; // 日租价格
    private BigDecimal pricePerNight; // 包夜价格
    private BigDecimal deposit; // 押金
    private String status; // ONLINE, OFFLINE, RENTED
    private Integer level;
    private String rank; // 段位
    private String region; // 区服
    private String[] tags; // 标签
    private String[] images; // 图片列表
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

