package com.zhk.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.user.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号 Mapper
 *
 * @author shigure
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}

