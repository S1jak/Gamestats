package com.lxw.gamestats.service.impl;

import com.lxw.common.constant.MessageConstant;
import com.lxw.common.context.BaseContext;
import com.lxw.common.exception.BaseException;
import com.lxw.common.exception.UserNotLoginException;
import com.lxw.gamestats.mapper.UserGameMapper;
import com.lxw.gamestats.mapper.UserMapper;
import com.lxw.gamestats.service.UserService;
import com.lxw.pojo.dto.PasswordUpdateDTO;
import com.lxw.pojo.dto.UserUpdateDTO;
import com.lxw.pojo.entity.User;
import com.lxw.pojo.vo.UserStatsVO;
import com.lxw.pojo.vo.UserVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 用户服务实现。
 * <p>
 * 所有方法均从 BaseContext.getCurrentId() 获取当前登录用户 ID。
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** custom_tags 列 JSON 序列化用 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${game.upload.path:./uploads}")
    private String uploadPath;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserGameMapper userGameMapper;

    @Override
    public UserVO getProfile() {
        User user = userMapper.getById(currentUserId());
        return toUserVO(user);
    }

    @Override
    public UserVO updateProfile(UserUpdateDTO userUpdateDTO) {
        Long userId = currentUserId();
        User user = new User();
        user.setId(userId);
        if (userUpdateDTO != null) {
            if (StringUtils.hasText(userUpdateDTO.getUsername())) {
                user.setUsername(userUpdateDTO.getUsername());
            }
            if (userUpdateDTO.getBio() != null) {
                user.setBio(userUpdateDTO.getBio());
            }
            if (StringUtils.hasText(userUpdateDTO.getAvatar())) {
                user.setAvatar(userUpdateDTO.getAvatar());
            }
            // 7 个新字段：null 表示不更新（保留原值），具体值才会被写入
            if (userUpdateDTO.getGender() != null) {
                user.setGender(userUpdateDTO.getGender());
            }
            if (userUpdateDTO.getBirthday() != null) {
                user.setBirthday(userUpdateDTO.getBirthday());
            }
            if (userUpdateDTO.getRegion() != null) {
                user.setRegion(userUpdateDTO.getRegion());
            }
            if (userUpdateDTO.getPreferredLanguage() != null) {
                user.setPreferredLanguage(userUpdateDTO.getPreferredLanguage());
            }
            if (userUpdateDTO.getProfileVisibility() != null) {
                user.setProfileVisibility(userUpdateDTO.getProfileVisibility());
            }
            if (userUpdateDTO.getAllowComments() != null) {
                user.setAllowComments(userUpdateDTO.getAllowComments());
            }
            if (userUpdateDTO.getEmailNotify() != null) {
                user.setEmailNotify(userUpdateDTO.getEmailNotify());
            }
            if (userUpdateDTO.getSiteNotify() != null) {
                user.setSiteNotify(userUpdateDTO.getSiteNotify());
            }
            // customTags 为 null 时不更新（保持原值）；为空 List 时清空标签（tagsToJson 返回空串）
            if (userUpdateDTO.getCustomTags() != null) {
                user.setCustomTags(tagsToJson(userUpdateDTO.getCustomTags()));
            }
        }
        userMapper.updateProfile(user);
        return toUserVO(userMapper.getById(userId));
    }

    @Override
    public UserStatsVO getStats() {
        return userGameMapper.getStatsByUserId(currentUserId());
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        String original = file.getOriginalFilename();
        String suffix = StringUtils.hasText(original) && original.contains(".")
                ? original.substring(original.lastIndexOf("."))
                : ".jpg";
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建上传目录失败: {}", dir.getAbsolutePath());
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        try {
            // 必须用绝对路径：MultipartFile.transferTo 对相对路径会解析到 java.io.tmpdir，
            // 导致目标目录不存在 → FileNotFoundException → 500
            File destFile = new File(dir.getAbsolutePath(), fileName);
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }

        String avatarUrl = "/uploads/" + fileName;

        // 同步更新 users.avatar
        User user = new User();
        user.setId(currentUserId());
        user.setAvatar(avatarUrl);
        userMapper.updateProfile(user);

        return avatarUrl;
    }

    @Override
    public void changePassword(PasswordUpdateDTO passwordUpdateDTO) {
        if (passwordUpdateDTO == null
                || !StringUtils.hasText(passwordUpdateDTO.getOldPassword())
                || !StringUtils.hasText(passwordUpdateDTO.getNewPassword())) {
            throw new BaseException("原密码与新密码不能为空");
        }
        Long userId = currentUserId();
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // OAuth 用户 password_hash 为 null，无法通过密码校验
        String oldMd5 = DigestUtils.md5DigestAsHex(passwordUpdateDTO.getOldPassword().getBytes());
        if (user.getPasswordHash() == null || !oldMd5.equals(user.getPasswordHash())) {
            throw new BaseException(MessageConstant.PASSWORD_ERROR);
        }
        String newMd5 = DigestUtils.md5DigestAsHex(passwordUpdateDTO.getNewPassword().getBytes());
        userMapper.updatePassword(userId, newMd5);
    }

    /** 获取当前登录用户 ID，未登录抛异常 */
    private Long currentUserId() {
        Long id = BaseContext.getCurrentId();
        if (id == null) {
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        return id;
    }

    /** User → UserVO（customTags 由 JSON 字符串反序列化为 List） */
    private UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        // BeanUtils 因类型不同不会复制 customTags，需手动反序列化
        userVO.setCustomTags(jsonToTags(user.getCustomTags()));
        return userVO;
    }

    /**
     * List&lt;String&gt; → JSON 字符串（存入 users.custom_tags）
     * 空列表返回空串而非 null，确保 updateProfile 的
     * &lt;if test="customTags != null"&gt; 能触发更新，从而支持"清空标签"操作。
     */
    private String tagsToJson(List<String> tags) {
        if (tags == null) {
            return null;
        }
        if (tags.isEmpty()) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(tags);
        } catch (Exception e) {
            log.warn("自定义标签序列化失败: {}", tags, e);
            return null;
        }
    }

    /** JSON 字符串 → List<String>（users.custom_tags 反序列化） */
    private List<String> jsonToTags(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("自定义标签反序列化失败: {}", json, e);
            return new ArrayList<>();
        }
    }
}
