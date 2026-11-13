package com.lxw.gamestats.service;

import com.lxw.pojo.dto.PasswordUpdateDTO;
import com.lxw.pojo.dto.UserUpdateDTO;
import com.lxw.pojo.vo.UserStatsVO;
import com.lxw.pojo.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 获取当前用户资料
     */
    UserVO getProfile();

    /**
     * 更新用户资料
     */
    UserVO updateProfile(UserUpdateDTO userUpdateDTO);

    /**
     * 获取用户游戏统计
     */
    UserStatsVO getStats();

    /**
     * 上传头像，返回可访问 URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 修改密码（校验原密码后更新）
     */
    void changePassword(PasswordUpdateDTO passwordUpdateDTO);
}
