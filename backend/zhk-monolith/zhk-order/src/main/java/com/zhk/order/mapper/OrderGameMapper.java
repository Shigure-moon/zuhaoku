package com.zhk.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.order.entity.Game;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游戏 Mapper（订单模块使用，避免循环依赖）
 *
 * @author shigure
 */
@Mapper
public interface OrderGameMapper extends BaseMapper<Game> {
}

