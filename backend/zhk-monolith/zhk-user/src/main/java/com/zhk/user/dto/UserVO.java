package com.zhk.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 *
 * @author shigure
 */
@Data
public class UserVO {
    private Long userId;
    private String nickname;
    private String mobile;
    private String avatarUrl;
    private String role;
    private Integer zhimaScore;
    private Integer status; // 状态：1-正常，2-冻结
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

