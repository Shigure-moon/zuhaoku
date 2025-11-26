package com.zhk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 用户实体类（订单模块使用，避免循环依赖）
 *
 * @author shigure
 */
@Data
@TableName("user")
@Alias("OrderUser")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickname;
    private String mobile;
    private String password;

    @TableField("idcard_hash")
    private String idcardHash;

    @TableField("zhima_score")
    private Integer zhimaScore;

    @TableField("avatar_url")
    private String avatarUrl;

    private String role;
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

