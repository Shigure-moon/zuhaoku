package com.zhk.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.user.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日志审计 Mapper 接口
 *
 * @author shigure
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}

