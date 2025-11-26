package com.zhk.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.risk.entity.AbnormalBehavior;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异常行为记录 Mapper 接口
 *
 * @author shigure
 */
@Mapper
public interface AbnormalBehaviorMapper extends BaseMapper<AbnormalBehavior> {
}

