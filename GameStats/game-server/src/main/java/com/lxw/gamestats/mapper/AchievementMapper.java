package com.lxw.gamestats.mapper;

import com.lxw.pojo.vo.AchievementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成就 Mapper（对应 achievements 表，关联 user_achievements 查询解锁状态）
 */
@Mapper
public interface AchievementMapper {

    /** 查询游戏的成就列表（含当前用户解锁状态，直接映射到 AchievementVO） */
    List<AchievementVO> listVOsByGameIdAndUserId(@Param("gameId") Long gameId, @Param("userId") Long userId);
}
