package com.zhk.risk.dto;

import lombok.Data;

/**
 * 风控检查结果DTO
 *
 * @author shigure
 */
@Data
public class RiskCheckResult {
    /**
     * 是否通过检查
     */
    private Boolean passed;

    /**
     * 风险等级：0-正常 1-低 2-中 3-高
     */
    private Integer riskLevel;

    /**
     * 风险原因
     */
    private String reason;

    /**
     * 是否需要人脸识别
     */
    private Boolean needFaceVerification;

    /**
     * 是否需要冻结账号
     */
    private Boolean needFreezeAccount;

    /**
     * 距离常用登录地的距离（公里）
     */
    private Double distance;
}

