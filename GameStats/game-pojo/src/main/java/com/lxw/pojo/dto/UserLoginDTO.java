package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 手机号密码登录请求
 */
@Data
public class UserLoginDTO implements Serializable {

    private String phone;

    private String password;
}
