package com.zhk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账号实体类（订单模块使用，避免循环依赖）
 *
 * @author shigure
 */
@Data
@TableName("account")
@Alias("OrderAccount")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("game_id")
    private Integer gameId;

    @TableField("owner_uid")
    private Long ownerUid;

    private String title;
    private String description;

    @TableField("username_enc")
    private String usernameEnc;

    @TableField("pwd_enc")
    private String pwdEnc;

    private String iv;
    private Integer lvl;
    private String skins;
    private BigDecimal deposit;

    @TableField("price_30min")
    private BigDecimal price30min;

    @TableField("price_1h")
    private BigDecimal price1h;

    @TableField("price_overnight")
    private BigDecimal priceOvernight;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

