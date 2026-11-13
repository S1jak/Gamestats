package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新用户资料请求
 */
@Data
public class UserUpdateDTO implements Serializable {

    private String username;

    private String bio;

    private String avatar;

    private String gender;

    private LocalDate birthday;

    private String region;

    private String preferredLanguage;

    private String profileVisibility;

    private Integer allowComments;

    private Integer emailNotify;

    private Integer siteNotify;

    /** 自定义标签列表（整体覆盖） */
    private List<String> customTags;
}
