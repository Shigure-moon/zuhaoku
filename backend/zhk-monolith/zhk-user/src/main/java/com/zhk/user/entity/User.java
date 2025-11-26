package com.zhk.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author shigure
 */
@Data
@TableName("user")
public class User {
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 身份证 SHA-256 哈希
     */
    private String idcardHash;

    /**
     * 芝麻信用分
     */
    private Integer zhimaScore;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 角色：TENANT-租客, OWNER-商家, OPERATOR-运营
     */
    private String role;

    /**
     * 状态：1-正常, 2-冻结
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

