package com.zhk.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.user.entity.Game;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游戏 Mapper
 *
 * @author shigure
 */
@Mapper
public interface GameMapper extends BaseMapper<Game> {
}

