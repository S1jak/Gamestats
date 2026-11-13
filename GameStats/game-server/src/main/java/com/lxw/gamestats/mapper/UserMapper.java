package com.lxw.gamestats.mapper;

import com.lxw.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper（对应 users 表）
 */
@Mapper
public interface UserMapper {

    /** 根据手机号查询（本地登录用） */
    User getByPhone(String phone);

    /** 根据 platform + platformId 查询（OAuth 登录用） */
    User getByPlatform(@Param("platform") String platform, @Param("platformId") String platformId);

    /** 根据主键查询 */
    User getById(Long id);

    /** 新增用户（OAuth 首次登录自动注册） */
    int insert(User user);

    /** 更新用户资料（动态字段：username / bio / avatar / level） */
    int updateProfile(User user);

    /** 修改密码（MD5 后入库） */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
