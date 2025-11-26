package com.zhk.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账号实体类
 *
 * @author shigure
 */
@Data
@TableName("account")
public class Account {
    /**
     * 账号ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 游戏ID
     */
    @TableField("game_id")
    private Integer gameId;

    /**
     * 号主用户ID
     */
    @TableField("owner_uid")
    private Long ownerUid;

    /**
     * 账号标题
     */
    private String title;

    /**
     * 账号描述
     */
    private String description;

    /**
     * 账号名（加密）
     */
    @TableField("username_enc")
    private String usernameEnc;

    /**
     * 密码（加密）
     */
    @TableField("pwd_enc")
    private String pwdEnc;

    /**
     * 初始化向量
     */
    private String iv;

    /**
     * 账号等级
     */
    private Integer lvl;

    /**
     * 皮肤信息（JSON格式）
     */
    private String skins;

    /**
     * 固定押金
     */
    private BigDecimal deposit;

    /**
     * 30分钟价格
     */
    @TableField("price_30min")
    private BigDecimal price30min;

    /**
     * 1小时价格
     */
    @TableField("price_1h")
    private BigDecimal price1h;

    /**
     * 包夜价格
     */
    @TableField("price_overnight")
    private BigDecimal priceOvernight;

    /**
     * 状态：1-上架, 2-下架, 3-租赁中
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

