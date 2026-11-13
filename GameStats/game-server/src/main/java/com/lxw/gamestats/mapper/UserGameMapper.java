package com.lxw.gamestats.mapper;

import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.GameGenreRow;
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

    /** 更新游戏状态（按 user_id + game_id 限定归属，并同步维护 completed_at） */
    int updateStatus(@Param("userId") Long userId, @Param("gameId") Long gameId, @Param("status") String status);

    /** 查询游戏的类型名称列表（用于 GameVO.genres / GameDetailVO.genres，按 game_genres.id 升序） */
    List<String> listGenreNamesByGameId(Long gameId);

    /** 一次性查询当前用户全部游戏的类型行（game_id + name，按 game_id, genre id 升序），供列表分组填充 genres */
    List<GameGenreRow> listGenreRowsByUserId(@Param("userId") Long userId);

    /**
     * 查询当前用户某款游戏最近 N 个自然日（含今天）的按日游玩时长
     *
     * @param offsetDays 区间起点相对今天的天数偏移，取 days - 1；SQL 用
     *                   DATE(start_time) &gt;= DATE_SUB(CURDATE(), INTERVAL offsetDays DAY)
     *                   保证区间为 [今天-(days-1), 今天]，不会多算 1 天
     */
    List<DailyPlayVO> listPlayTrendByUserIdAndGameId(@Param("userId") Long userId,
                                                     @Param("gameId") Long gameId,
                                                     @Param("offsetDays") Integer offsetDays);
}
