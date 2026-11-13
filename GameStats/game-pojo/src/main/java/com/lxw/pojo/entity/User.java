package com.lxw.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 users 表）
 */
@Data
public class User implements Serializable {

    private Long id;

    /** 用户名 */
    private String username;

    /** 头像 URL */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号（本地登录用） */
    private String phone;

    /** 密码（MD5） */
    private String passwordHash;

    /** 个人简介 */
    private String bio;

    /** 等级 */
    private Integer level;

    /** 加入日期 */
    private LocalDate joinDate;

    /** 自定义标签（JSON 数组字符串，如 ["硬核玩家","成就猎人"]） */
    private String customTags;

    /** 第三方登录提供商 */
    private String platform;

    /** 第三方平台 ID */
    private String platformId;

    /** 第三方 access_token */
    private String accessToken;

    /** 第三方 refresh_token */
    private String refreshToken;

    /** token 过期时间 */
    private LocalDateTime tokenExpiresAt;

    /** 性别：male / female / other */
    private String gender;

    /** 生日 */
    private LocalDate birthday;

    /** 地区（如 "北京" / "上海" / "广东"） */
    private String region;

    /** 偏好语言：zh-CN / en-US */
    private String preferredLanguage;

    /** 资料可见性：public / friends / private */
    private String profileVisibility;

    /** 是否允许评论：0/1 */
    private Integer allowComments;

    /** 是否接收邮件通知：0/1 */
    private Integer emailNotify;

    /** 是否接收站内通知：0/1 */
    private Integer siteNotify;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
