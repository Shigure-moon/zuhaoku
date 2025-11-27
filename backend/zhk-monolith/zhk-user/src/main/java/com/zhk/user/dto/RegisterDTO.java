package com.zhk.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求 DTO
 *
 * @author shigure
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    // 验证码暂时设为可选（开发环境），生产环境需要实现验证码功能
    // @NotBlank(message = "验证码不能为空")
    private String verifyCode;
}

