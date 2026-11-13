package com.lxw.gamestats.service;

import com.lxw.pojo.dto.GameNoteDTO;
import com.lxw.pojo.dto.GameStatusDTO;
import com.lxw.pojo.dto.GameTagsDTO;
import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.GameDetailVO;
import com.lxw.pojo.vo.GameVO;

import java.util.List;

/**
 * 游戏服务
 */
public interface GameService {

    /**
     * 获取游戏列表
     */
    List<GameVO> list();

    /**
     * 获取游戏详情
     */
    GameDetailVO getById(Long id);

    /**
     * 更新游戏笔记
     */
    GameVO updateNote(Long gameId, GameNoteDTO gameNoteDTO);

    /**
     * 更新游戏状态
     */
    GameVO updateStatus(Long gameId, GameStatusDTO gameStatusDTO);

    /**
     * 全量替换当前用户对某款游戏的标签
     *
     * @param gameId      游戏 ID，必须属于当前用户
     * @param gameTagsDTO 标签请求体，tags 为 null 视为非法
     * @return 更新后的游戏视图（含用户标签与全部类型）
     */
    GameVO updateTags(Long gameId, GameTagsDTO gameTagsDTO);

    /**
     * 查询当前用户某款游戏最近 N 个自然日（含今天）的按日游玩时长
     *
     * @param gameId 游戏 ID，必须属于当前用户
     * @param days   天数，默认 30，取值 1-365
     * @return 仅含有记录的日期，按日期升序；无记录返回空列表
     */
    List<DailyPlayVO> listPlayTrend(Long gameId, Integer days);
}