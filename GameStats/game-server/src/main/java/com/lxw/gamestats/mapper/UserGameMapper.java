package com.lxw.gamestats.mapper;

import com.lxw.pojo.vo.GameVO;
import com.lxw.pojo.vo.UserStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户游戏库 Mapper（对应 user_games 表，关联 games / play_records / achievements 等聚合查询）
 */
@Mapper
public interface UserGameMapper {

    /** 获取用户的游戏列表（多表关联 + 子查询聚合，直接映射到 GameVO） */
    List<GameVO> listGameVOsByUserId(Long userId);

    /** 获取单款游戏基本信息（按 game.id + user_id 校验归属，映射到 GameVO） */
    GameVO getGameVOByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** 用户游戏统计（聚合查询，直接映射到 UserStatsVO） */
    UserStatsVO getStatsByUserId(Long userId);

    /** 更新游戏笔记（按 user_id + game_id 限定归属） */
    int updateNote(@Param("userId") Long userId, @Param("gameId") Long gameId, @Param("note") String note);

    /** 更新游戏状态（按 user_id + game_id 限定归属） */
    int updateStatus(@Param("userId") Long userId, @Param("gameId") Long gameId, @Param("status") String status);

    /** 查询游戏的类型名称列表（用于 GameVO.tags） */
    List<String> listGenreNamesByGameId(Long gameId);
}
