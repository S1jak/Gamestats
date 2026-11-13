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

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    /** 头像大小上限 2MB，与 spring.servlet.multipart.max-file-size 保持一致 */
    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

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
        // 全部校验通过后才落盘，落盘扩展名由魔数推断，不使用客户端文件名
        String suffix = validateAvatar(file);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;

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

    /**
     * 头像上传校验，全部通过返回由魔数推断的扩展名。
     * <p>
     * 校验顺序：非空（含 0 字节）→ 大小 ≤2MB → 扩展名白名单 → Content-Type 白名单
     * → 魔数与声明类型一致 → 内容可解码（WEBP 仅靠文件头）。
     * 任一失败抛 BaseException，调用方不得落盘或修改 users.avatar。
     */
    private String validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.UPLOAD_EMPTY);
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BaseException(MessageConstant.UPLOAD_EMPTY);
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BaseException(MessageConstant.UPLOAD_TOO_LARGE);
        }
        String declaredExtension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(declaredExtension)) {
            throw new BaseException(MessageConstant.UPLOAD_INVALID_FORMAT);
        }
        String contentType = normalizedContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BaseException(MessageConstant.UPLOAD_INVALID_FORMAT);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        String detectedExtension = detectExtensionByMagic(bytes);
        if (detectedExtension == null
                || !isDeclaredTypeMatched(detectedExtension, declaredExtension, contentType)) {
            throw new BaseException(MessageConstant.UPLOAD_INVALID_FORMAT);
        }
        // WEBP 无 ImageIO 解码器，其内容有效性已由 RIFF....WEBP 文件头判定
        if (!"webp".equals(detectedExtension) && !isDecodableImage(bytes)) {
            throw new BaseException(MessageConstant.UPLOAD_INVALID_FORMAT);
        }
        return detectedExtension;
    }

    /** 仅取文件名最后一段的扩展名（忽略路径），统一小写 */
    private String extensionOf(String originalFilename) {
        String name = originalFilename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Content-Type 去掉 charset 等参数后统一小写 */
    private String normalizedContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String value = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 通过文件头魔数识别真实图片类型，无法识别返回 null */
    private String detectExtensionByMagic(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "png";
        }
        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38) {
            return "gif";
        }
        if (bytes.length >= 12
                && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45
                && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) {
            return "webp";
        }
        return null;
    }

    /** 魔数推断类型必须与声明扩展名、Content-Type 一致 */
    private boolean isDeclaredTypeMatched(String detectedExtension, String declaredExtension, String contentType) {
        return switch (detectedExtension) {
            case "jpg" -> ("jpg".equals(declaredExtension) || "jpeg".equals(declaredExtension))
                    && "image/jpeg".equals(contentType);
            case "png" -> "png".equals(declaredExtension) && "image/png".equals(contentType);
            case "gif" -> "gif".equals(declaredExtension) && "image/gif".equals(contentType);
            case "webp" -> "webp".equals(declaredExtension) && "image/webp".equals(contentType);
            default -> false;
        };
    }

    /** ImageIO 无法解析即视为伪造图片 */
    private boolean isDecodableImage(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes)) != null;
        } catch (IOException e) {
            log.warn("图片内容校验失败: {}", e.getMessage());
            return false;
        }
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
