package com.zhk.risk.event;

import lombok.Data;

/**
 * 登录成功事件
 *
 * @author shigure
 */
@Data
public class LoginSuccessEvent {
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;

    public LoginSuccessEvent(Long userId, String ipAddress, String userAgent, String deviceFingerprint) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceFingerprint = deviceFingerprint;
    }
}

