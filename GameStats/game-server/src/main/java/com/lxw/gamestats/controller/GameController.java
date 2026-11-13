package com.lxw.gamestats.controller;

import com.lxw.gamestats.service.GameService;
import com.lxw.pojo.dto.GameNoteDTO;
import com.lxw.pojo.dto.GameStatusDTO;
import com.lxw.pojo.vo.GameDetailVO;
import com.lxw.pojo.vo.GameVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 游戏控制器
 * <ul>
 *   <li>3.1 GET /api/games              获取游戏列表（需鉴权）</li>
 *   <li>3.2 GET /api/games/{id}         获取游戏详情（需鉴权）</li>
 *   <li>3.3 PUT /api/games/{gameId}/note   更新游戏笔记（需鉴权）</li>
 *   <li>3.4 PUT /api/games/{gameId}/status 更新游戏状态（需鉴权）</li>
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
}
