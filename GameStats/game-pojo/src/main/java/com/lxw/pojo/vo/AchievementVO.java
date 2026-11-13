package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 成就视图对象
 */
@Data
public class AchievementVO implements Serializable {

    private Long id;

    private String name;

    private String description;

    private String icon;

    private Boolean unlocked;

    private LocalDate unlockedDate;
}
