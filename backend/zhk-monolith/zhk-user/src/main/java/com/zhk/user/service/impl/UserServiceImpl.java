package com.zhk.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhk.common.security.JwtUtil;
import com.zhk.common.web.BusinessException;
import com.zhk.user.dto.LoginDTO;
import com.zhk.user.dto.LoginVO;
import com.zhk.user.dto.RegisterDTO;
import com.zhk.user.dto.UserVO;
import com.zhk.user.entity.User;
import com.zhk.user.mapper.UserMapper;
import com.zhk.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UserVO register(RegisterDTO dto) {
        // 检查手机号是否已注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMobile, dto.getMobile());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "手机号已注册");
        }

        // 创建用户
        User user = new User();
        user.setMobile(dto.getMobile());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole("TENANT");
        user.setStatus(1);

        userMapper.insert(user);

        // 转换为 VO
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setUserId(user.getId());
        return vo;
    }

    @Override
    public LoginVO login(LoginDTO dto, String ipAddress, String userAgent) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMobile, dto.getMobile());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.warn("登录失败：用户不存在，手机号: {}", dto.getMobile());
            throw new BusinessException(401, "手机号或密码错误");
        }

        // 验证密码
        boolean passwordMatches = passwordEncoder.matches(dto.getPassword(), user.getPassword());
        log.debug("密码验证 - 手机号: {}, 匹配结果: {}, 存储哈希: {}", 
            dto.getMobile(), passwordMatches, user.getPassword() != null ? user.getPassword().substring(0, 20) + "..." : "null");
        
        if (!passwordMatches) {
            log.warn("登录失败：密码错误，手机号: {}", dto.getMobile());
            throw new BusinessException(401, "手机号或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被冻结");
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getMobile(), user.getRole());

        // 构建响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setUserId(user.getId());
        loginVO.setUserInfo(userVO);

        // 发布登录成功事件，触发风控检查（异步执行，不阻塞登录）
        // 使用 Spring 事件机制，避免直接依赖 zhk-risk 模块
        if (ipAddress != null) {
            try {
                // 使用反射创建事件对象，避免编译时依赖
                Class<?> eventClass = Class.forName("com.zhk.risk.event.LoginSuccessEvent");
                Object event = eventClass.getConstructor(Long.class, String.class, String.class, String.class)
                        .newInstance(user.getId(), ipAddress, userAgent, null);
                eventPublisher.publishEvent(event);
                log.debug("已发布登录成功事件: userId={}, ip={}", user.getId(), ipAddress);
            } catch (ClassNotFoundException e) {
                log.debug("风控模块未加载，跳过风控检查");
                // 风控模块可能未加载，不影响登录流程
            } catch (Exception e) {
                log.warn("发布登录成功事件失败: {}", e.getMessage());
                // 事件发布失败不影响登录流程
            }
        }

        return loginVO;
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setUserId(user.getId());
        return vo;
    }

    @Override
    public UserVO getUserByMobile(String mobile) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMobile, mobile);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return null;
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setUserId(user.getId());
        return vo;
    }
}

