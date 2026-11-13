package com.lxw.gamestats.mapper;

import com.lxw.pojo.vo.PlatformVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户游戏平台拥有 Mapper（对应 user_game_platforms 表）
 */
@Mapper
public interface UserGamePlatformMapper {

    /** 查询用户在某款游戏上的平台拥有情况（直接映射到 PlatformVO） */
    List<PlatformVO> listVOsByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);
}
