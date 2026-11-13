package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * OAuth 跳转 URL 视图对象
 */
@Data
public class AuthUrlVO implements Serializable {

    /** 第三方授权地址 */
    private String authUrl;

    public AuthUrlVO() {
    }

    public AuthUrlVO(String authUrl) {
        this.authUrl = authUrl;
    }
}
