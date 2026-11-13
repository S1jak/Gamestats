package com.lxw.gamestats.mapper;

import com.lxw.pojo.entity.UserGameTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户游戏标签 Mapper（对应 user_game_tags 表）
 */
@Mapper
public interface UserGameTagMapper {

    /** 查询当前用户某款游戏的全部标签（按 id 升序，即插入顺序） */
    List<UserGameTag> listByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);

    /** 一次性查询当前用户全部标签（按 game_id, id 升序），供列表分组填充，避免 N+1 */
    List<UserGameTag> listByUserId(@Param("userId") Long userId);

    /** 删除当前用户某款游戏的全部标签（全量替换前先删） */
    int deleteByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);

    /** 批量插入标签；调用方需保证列表非空且已去重 */
    int batchInsert(@Param("tags") List<UserGameTag> tags);
}