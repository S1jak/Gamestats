package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.UserService;
import com.lxw.pojo.dto.PasswordUpdateDTO;
import com.lxw.pojo.dto.UserUpdateDTO;
import com.lxw.pojo.vo.UserStatsVO;
import com.lxw.pojo.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 * <ul>
 *   <li>2.1 GET /api/user/profile   获取当前用户资料（需鉴权）</li>
 *   <li>2.2 PUT /api/user/profile   更新用户资料（需鉴权）</li>
 *   <li>2.3 GET /api/user/stats     获取用户游戏统计（需鉴权）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /** 2.1 获取当前用户资料 */
    @GetMapping("/profile")
    public UserVO getProfile() {
        return userService.getProfile();
    }

    /** 2.2 更新用户资料 */
    @PutMapping("/profile")
    public UserVO updateProfile(@RequestBody UserUpdateDTO userUpdateDTO) {
        return userService.updateProfile(userUpdateDTO);
    }

    /** 2.3 获取用户游戏统计 */
    @GetMapping("/stats")
    public UserStatsVO getStats() {
        return userService.getStats();
    }

    /** 2.4 修改密码（校验原密码后更新为新密码） */
    @PutMapping("/password")
    public void changePassword(@RequestBody PasswordUpdateDTO passwordUpdateDTO) {
        userService.changePassword(passwordUpdateDTO);
    }
}
