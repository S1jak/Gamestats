package com.lxw.gamestats.service.impl;

import com.lxw.common.constant.MessageConstant;
import com.lxw.common.context.BaseContext;
import com.lxw.common.exception.BaseException;
import com.lxw.common.exception.UserNotLoginException;
import com.lxw.gamestats.mapper.AchievementMapper;
import com.lxw.gamestats.mapper.UserGameMapper;
import com.lxw.gamestats.mapper.UserGamePlatformMapper;
import com.lxw.gamestats.service.GameService;
import com.lxw.pojo.dto.GameNoteDTO;
import com.lxw.pojo.dto.GameStatusDTO;
import com.lxw.pojo.vo.AchievementVO;
import com.lxw.pojo.vo.GameDetailVO;
import com.lxw.pojo.vo.GameVO;
import com.lxw.pojo.vo.PlatformVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 游戏服务实现。
 * <p>
 * 所有方法均从 BaseContext.getCurrentId() 获取当前登录用户 ID。
 * GameVO.tags 通过 listGenreNamesByGameId 单独查询填充。
 */
@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private UserGameMapper userGameMapper;

    @Autowired
    private AchievementMapper achievementMapper;

    @Autowired
    private UserGamePlatformMapper userGamePlatformMapper;

    @Override
    public List<GameVO> list() {
        Long userId = currentUserId();
        List<GameVO> games = userGameMapper.listGameVOsByUserId(userId);
        if (games != null) {
            for (GameVO game : games) {
                game.setTags(userGameMapper.listGenreNamesByGameId(game.getId()));
            }
        }
        return games;
    }

    @Override
    public GameDetailVO getById(Long id) {
        Long userId = currentUserId();
        GameVO gameVO = userGameMapper.getGameVOByIdAndUserId(id, userId);
        if (gameVO == null) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }

        GameDetailVO detail = new GameDetailVO();
        BeanUtils.copyProperties(gameVO, detail);

        // 填充 tags / platforms / achievementList
        detail.setTags(userGameMapper.listGenreNamesByGameId(id));
        detail.setPlatforms(userGamePlatformMapper.listVOsByUserIdAndGameId(userId, id));
        detail.setAchievementList(achievementMapper.listVOsByGameIdAndUserId(id, userId));

        return detail;
    }

    @Override
    public GameVO updateNote(Long gameId, GameNoteDTO gameNoteDTO) {
        Long userId = currentUserId();
        String note = gameNoteDTO == null ? null : gameNoteDTO.getNote();
        int rows = userGameMapper.updateNote(userId, gameId, note);
        if (rows == 0) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }
        return reloadGameVO(gameId, userId);
    }

    @Override
    public GameVO updateStatus(Long gameId, GameStatusDTO gameStatusDTO) {
        Long userId = currentUserId();
        if (gameStatusDTO == null || gameStatusDTO.getStatus() == null) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }
        int rows = userGameMapper.updateStatus(userId, gameId, gameStatusDTO.getStatus());
        if (rows == 0) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }
        return reloadGameVO(gameId, userId);
    }

    /** 重新查询游戏信息并填充 tags */
    private GameVO reloadGameVO(Long gameId, Long userId) {
        GameVO gameVO = userGameMapper.getGameVOByIdAndUserId(gameId, userId);
        if (gameVO != null) {
            gameVO.setTags(userGameMapper.listGenreNamesByGameId(gameId));
        }
        return gameVO;
    }

    /** 获取当前登录用户 ID，未登录抛异常 */
    private Long currentUserId() {
        Long id = BaseContext.getCurrentId();
        if (id == null) {
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        return id;
    }
}
