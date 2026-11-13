package com.lxw.gamestats.service.impl;

import com.lxw.common.constant.JwtClaimsConstant;
import com.lxw.common.constant.MessageConstant;
import com.lxw.common.exception.LoginFailedException;
import com.lxw.common.properties.JwtProperties;
import com.lxw.common.utils.JwtUtil;
import com.lxw.gamestats.mapper.UserMapper;
import com.lxw.gamestats.service.AuthService;
import com.lxw.pojo.dto.UserLoginDTO;
import com.lxw.pojo.entity.User;
import com.lxw.pojo.vo.AuthUrlVO;
import com.lxw.pojo.vo.LoginVO;
import com.lxw.pojo.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现。
 * <p>
 * 手机号登录：通过 users.phone + password_hash(MD5) 校验。
 * OAuth 回调：通过 (platform, platformId) 查找用户，不存在则自动注册。
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserMapper userMapper;

    @Override
    public AuthUrlVO getAuthUrl(String provider) {
        // TODO 接入真实 OAuth，按 provider 构造授权地址
        String authUrl = "https://" + provider + ".example.com/oauth/authorize"
                + "?client_id=gamestats&redirect_uri=/api/auth/callback/" + provider
                + "&response_type=code";
        return new AuthUrlVO(authUrl);
    }

    @Override
    public LoginVO callback(String provider, String code) {
        // Mock：用 code 充当 platformId，真实场景应先用 code 换取 access_token 再拉取用户信息
        String platformId = code;
        User user = userMapper.getByPlatform(provider, platformId);
        if (user == null) {
            // 首次 OAuth 登录，自动注册
            user = new User();
            user.setUsername(provider + "_user");
            user.setPlatform(provider);
            user.setPlatformId(platformId);
            user.setLevel(1);
            user.setJoinDate(LocalDate.now());
            userMapper.insert(user);
        }
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(UserLoginDTO userLoginDTO) {
        if (userLoginDTO == null
                || userLoginDTO.getPhone() == null
                || userLoginDTO.getPassword() == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        User user = userMapper.getByPhone(userLoginDTO.getPhone());
        if (user == null) {
            throw new LoginFailedException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        String md5Password = DigestUtils.md5DigestAsHex(userLoginDTO.getPassword().getBytes());
        if (!md5Password.equals(user.getPasswordHash())) {
            throw new LoginFailedException(MessageConstant.PASSWORD_ERROR);
        }
        return buildLoginVO(user);
    }

    /** 生成 JWT 并组装 LoginVO */
    private LoginVO buildLoginVO(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(userVO);
        return loginVO;
    }
}
