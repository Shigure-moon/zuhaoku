package com.zhk.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.risk.entity.UserLoginRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户登录记录 Mapper 接口
 *
 * @author shigure
 */
@Mapper
public interface UserLoginRecordMapper extends BaseMapper<UserLoginRecord> {
}

