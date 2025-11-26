package com.zhk.user.service;

import com.zhk.user.dto.LoginDTO;
import com.zhk.user.dto.LoginVO;
import com.zhk.user.dto.RegisterDTO;
import com.zhk.user.dto.UserVO;

/**
 * 用户服务接口
 *
 * @author shigure
 */
public interface UserService {
    /**
     * 用户注册
     */
    UserVO register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto, String ipAddress, String userAgent);

    /**
     * 根据ID获取用户信息
     */
    UserVO getUserById(Long userId);

    /**
     * 根据手机号获取用户
     */
    UserVO getUserByMobile(String mobile);
}

