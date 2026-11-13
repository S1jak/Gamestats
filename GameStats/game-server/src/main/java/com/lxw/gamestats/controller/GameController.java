package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.GameService;
import com.lxw.pojo.dto.GameNoteDTO;
import com.lxw.pojo.dto.GameStatusDTO;
import com.lxw.pojo.dto.GameTagsDTO;
import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.GameDetailVO;
import com.lxw.pojo.vo.GameVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 游戏控制器
 * <ul>
 *   <li>3.1 GET /api/games              获取游戏列表（需鉴权）</li>
 *   <li>3.2 GET /api/games/{id}         获取游戏详情（需鉴权）</li>
 *   <li>3.3 PUT /api/games/{gameId}/note   更新游戏笔记（需鉴权）</li>
 *   <li>3.4 PUT /api/games/{gameId}/status 更新游戏状态（需鉴权）</li>
 *   <li>3.5 PUT /api/games/{gameId}/tags   全量替换游戏标签（需鉴权）</li>
 *   <li>3.6 GET /api/games/{gameId}/play-trend 游玩趋势（需鉴权）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    /** 3.1 获取游戏列表 */
    @GetMapping
    public List<GameVO> list() {
        return gameService.list();
    }

    /** 3.2 获取游戏详情 */
    @GetMapping("/{id}")
    public GameDetailVO getById(@PathVariable Long id) {
        return gameService.getById(id);
    }

    /** 3.3 更新游戏笔记 */
    @PutMapping("/{gameId}/note")
    public GameVO updateNote(@PathVariable Long gameId, @RequestBody GameNoteDTO gameNoteDTO) {
        return gameService.updateNote(gameId, gameNoteDTO);
    }

    /** 3.4 更新游戏状态 */
    @PutMapping("/{gameId}/status")
    public GameVO updateStatus(@PathVariable Long gameId, @RequestBody GameStatusDTO gameStatusDTO) {
        return gameService.updateStatus(gameId, gameStatusDTO);
    }

    /** 3.5 全量替换游戏标签（空数组 = 清空） */
    @PutMapping("/{gameId}/tags")
    public GameVO updateTags(@PathVariable Long gameId, @RequestBody GameTagsDTO gameTagsDTO) {
        return gameService.updateTags(gameId, gameTagsDTO);
    }

    /** 3.6 游玩趋势：按日聚合最近 N 天（含今天），days 默认 30、取值 1-365 */
    @GetMapping("/{gameId}/play-trend")
    public List<DailyPlayVO> listPlayTrend(@PathVariable Long gameId,
                                           @RequestParam(defaultValue = "30") Integer days) {
        return gameService.listPlayTrend(gameId, days);
    }
}