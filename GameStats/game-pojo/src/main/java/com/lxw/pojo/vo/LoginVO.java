package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回（含 token）
 */
@Data
public class LoginVO implements Serializable {

    /** JWT token */
    private String token;

    /** 当前用户资料 */
    private UserVO user;
}
