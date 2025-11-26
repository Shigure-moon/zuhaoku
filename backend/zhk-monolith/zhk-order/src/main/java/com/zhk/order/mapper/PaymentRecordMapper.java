package com.zhk.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.order.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 *
 * @author shigure
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}

