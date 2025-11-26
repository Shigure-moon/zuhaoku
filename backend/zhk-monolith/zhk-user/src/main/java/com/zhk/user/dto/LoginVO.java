package com.zhk.user.dto;

import lombok.Data;

/**
 * 登录响应 VO
 *
 * @author shigure
 */
@Data
public class LoginVO {
    /**
     * Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO userInfo;
}

