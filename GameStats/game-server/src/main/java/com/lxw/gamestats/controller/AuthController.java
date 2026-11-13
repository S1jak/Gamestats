package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.AuthService;
import com.lxw.pojo.dto.UserLoginDTO;
import com.lxw.pojo.vo.AuthUrlVO;
import com.lxw.pojo.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * <ul>
 *   <li>1.1 GET  /api/auth/{provider}             获取 OAuth 跳转 URL（免鉴权）</li>
 *   <li>1.2 GET  /api/auth/callback/{provider}    OAuth 回调换取 token（免鉴权）</li>
 *   <li>      POST /api/auth/login                手机号密码登录（免鉴权，前端主登录方式）</li>
 * </ul>
 * 注：响应直接返回业务对象，与前端 axios 拦截器约定一致（前端取 response.data 作为业务数据）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /** 1.1 获取 OAuth 跳转 URL */
    @GetMapping("/{provider}")
    public AuthUrlVO getAuthUrl(@PathVariable String provider) {
        return authService.getAuthUrl(provider);
    }

    /** 1.2 OAuth 回调换取 token */
    @GetMapping("/callback/{provider}")
    public LoginVO callback(@PathVariable String provider,
                            @RequestParam(required = false) String code) {
        return authService.callback(provider, code);
    }

    /** 手机号密码登录（前端 user.loginWithPhone 调用） */
    @PostMapping("/login")
    public LoginVO login(@RequestBody UserLoginDTO userLoginDTO) {
        return authService.login(userLoginDTO);
    }
}
