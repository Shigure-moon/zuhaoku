package com.zhk.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.risk.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 黑名单 Mapper 接口
 *
 * @author shigure
 */
@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
}

