package com.zhk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 游戏实体类（订单模块使用，避免循环依赖）
 *
 * @author shigure
 */
@Data
@TableName("game")
@Alias("OrderGame")
public class Game {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
    private String publisher;

    @TableField("login_type")
    private String loginType;

    @TableField("icon_url")
    private String iconUrl;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

