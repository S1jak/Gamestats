package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户资料视图对象
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String avatar;

    private String bio;

    private Integer level;

    private LocalDate joinDate;

    /** 自定义标签列表 */
    private List<String> customTags;

    /** 性别 */
    private String gender;

    /** 生日 */
    private LocalDate birthday;

    /** 地区 */
    private String region;

    /** 偏好语言 */
    private String preferredLanguage;

    /** 资料可见性 */
    private String profileVisibility;

    /** 是否允许评论 */
    private Integer allowComments;

    /** 是否接收邮件通知 */
    private Integer emailNotify;

    /** 是否接收站内通知 */
    private Integer siteNotify;
}
