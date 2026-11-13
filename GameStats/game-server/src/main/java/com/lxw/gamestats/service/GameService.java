package com.lxw.gamestats.service;

import com.lxw.pojo.dto.GameNoteDTO;
import com.lxw.pojo.dto.GameStatusDTO;
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
}
