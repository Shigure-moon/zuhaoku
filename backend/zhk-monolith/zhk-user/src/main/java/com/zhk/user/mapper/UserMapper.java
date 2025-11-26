package com.zhk.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * @author shigure
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

