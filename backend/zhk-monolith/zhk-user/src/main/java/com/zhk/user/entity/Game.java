package com.zhk.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游戏实体类
 *
 * @author shigure
 */
@Data
@TableName("game")
public class Game {
    /**
     * 游戏ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 游戏名称
     */
    private String name;

    /**
     * 发行商
     */
    private String publisher;

    /**
     * 登录方式：qr-二维码, pwd-密码, token-令牌
     */
    @TableField("login_type")
    private String loginType;

    /**
     * 游戏图标URL
     */
    @TableField("icon_url")
    private String iconUrl;

    /**
     * 状态：1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

