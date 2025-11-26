package com.zhk.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.order.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号 Mapper（订单模块使用，避免循环依赖）
 *
 * @author shigure
 */
@Mapper
public interface OrderAccountMapper extends BaseMapper<Account> {
}

