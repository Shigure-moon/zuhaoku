package com.zhk.order.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉展示 VO
 *
 * @author shigure
 */
@Data
public class AppealVO {
    private Long id;
    private Long orderId;
    private Integer type;
    private String typeName; // 账号异常, 押金争议, 其他
    private List<String> evidenceUrls;
    private Integer verdict;
    private String verdictName; // 支持租客, 支持号主, 各担一半
    private Long operatorUid;
    private String operatorName;
    private LocalDateTime createTime;
    private LocalDateTime resolveTime;
}

