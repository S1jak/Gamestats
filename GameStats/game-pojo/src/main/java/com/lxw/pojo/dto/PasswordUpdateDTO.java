package com.lxw.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 */
@Data
public class PasswordUpdateDTO implements Serializable {

    /** 原密码（明文，后端 MD5 校验） */
    private String oldPassword;

    /** 新密码（明文，后端 MD5 加密后入库） */
    private String newPassword;
}
