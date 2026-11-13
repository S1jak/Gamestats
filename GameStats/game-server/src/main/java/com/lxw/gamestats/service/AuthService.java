package com.lxw.gamestats.service;

import com.lxw.pojo.dto.UserLoginDTO;
import com.lxw.pojo.vo.AuthUrlVO;
import com.lxw.pojo.vo.LoginVO;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 获取 OAuth 跳转 URL
     */
    AuthUrlVO getAuthUrl(String provider);

    /**
     * OAuth 回调换取 token
     */
    LoginVO callback(String provider, String code);

    /**
     * 手机号密码登录
     */
    LoginVO login(UserLoginDTO userLoginDTO);
}
