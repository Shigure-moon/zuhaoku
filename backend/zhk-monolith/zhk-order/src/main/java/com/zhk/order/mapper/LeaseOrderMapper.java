package com.zhk.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.order.entity.LeaseOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租赁订单 Mapper
 *
 * @author shigure
 */
@Mapper
public interface LeaseOrderMapper extends BaseMapper<LeaseOrder> {
}

