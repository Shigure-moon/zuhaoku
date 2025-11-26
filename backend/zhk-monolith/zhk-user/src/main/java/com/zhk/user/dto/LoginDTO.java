package com.zhk.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 *
 * @author shigure
 */
@Data
public class LoginDTO {
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @NotBlank(message = "密码不能为空")
    private String password;
}

