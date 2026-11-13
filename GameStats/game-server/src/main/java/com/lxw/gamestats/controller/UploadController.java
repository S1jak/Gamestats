package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 上传控制器
 * <ul>
 *   <li>2.4 POST /api/upload/avatar 上传头像（需鉴权）</li>
 * </ul>
 * <p>
 * 接口 2.4 当前由前端 el-upload 直接触发，未走 axios 拦截器。
 * 本服务已采用「前端改造」方案：在 el-upload 上携带 Authorization 头，
 * 同时 JwtInterceptor 兼容 query 参数 token，保证鉴权可用。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private UserService userService;

    /** 2.4 上传头像，返回可访问 URL */
    @PostMapping("/avatar")
    public Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = userService.uploadAvatar(file);
        return Map.of("url", url);
    }
}
