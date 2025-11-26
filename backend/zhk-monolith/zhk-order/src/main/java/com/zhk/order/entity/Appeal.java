package com.zhk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申诉实体类
 *
 * @author shigure
 */
@Data
@TableName("appeal")
public class Appeal {
    /**
     * 申诉ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 申诉类型：1-账号异常, 2-押金争议, 3-其他, 4-玩家恶意使用/销毁资源, 5-买家脚本盗号
     */
    private Integer type;

    /**
     * 证据URL列表（JSON格式）
     */
    @TableField("evidence_urls")
    private String evidenceUrls;

    /**
     * 裁决结果：1-支持租客, 2-支持号主, 3-各担一半
     */
    private Integer verdict;

    /**
     * 处理人用户ID
     */
    @TableField("operator_uid")
    private Long operatorUid;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 处理完成时间
     */
    @TableField("resolve_time")
    private LocalDateTime resolveTime;
}
