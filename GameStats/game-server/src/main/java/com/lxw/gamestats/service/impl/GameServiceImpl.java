package com.lxw.gamestats.service.impl;

import com.lxw.common.constant.GameStatusConstant;
import com.lxw.common.constant.MessageConstant;
import com.lxw.common.context.BaseContext;
import com.lxw.common.exception.BaseException;
import com.lxw.common.exception.UserNotLoginException;
import com.lxw.gamestats.mapper.AchievementMapper;
import com.lxw.gamestats.mapper.UserGameMapper;
import com.lxw.gamestats.mapper.UserGamePlatformMapper;
import com.lxw.gamestats.mapper.UserGameTagMapper;
import com.lxw.gamestats.service.GameService;
import com.lxw.pojo.dto.GameNoteDTO;
import com.lxw.pojo.dto.GameStatusDTO;
import com.lxw.pojo.dto.GameTagsDTO;
import com.lxw.pojo.entity.UserGameTag;
import com.lxw.pojo.vo.DailyPlayVO;
import com.lxw.pojo.vo.GameDetailVO;
import com.lxw.pojo.vo.GameGenreRow;
import com.lxw.pojo.vo.GameVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 游戏服务实现。
 * <p>
 * 所有方法均从 BaseContext.getCurrentId() 获取当前登录用户 ID。
 * 字段语义：tags = 当前用户自定义标签（user_game_tags），genres = 游戏全部类型，genre = 主类型。
 */
@Service
public class GameServiceImpl implements GameService {

    /** 游玩趋势默认天数 */
    private static final int DEFAULT_TREND_DAYS = 30;

    /** 游玩趋势天数下限 */
    private static final int MIN_TREND_DAYS = 1;

    /** 游玩趋势天数上限 */
    private static final int MAX_TREND_DAYS = 365;

    /** 单款游戏标签数量上限 */
    private static final int MAX_TAG_COUNT = 10;

    /** 单个标签最大字符数（按 Unicode 码点计） */
    private static final int MAX_TAG_LENGTH = 20;

    @Autowired
    private UserGameMapper userGameMapper;

    @Autowired
    private UserGameTagMapper userGameTagMapper;

    @Autowired
    private AchievementMapper achievementMapper;

    @Autowired
    private UserGamePlatformMapper userGamePlatformMapper;

    @Override
    public List<GameVO> list() {
        Long userId = currentUserId();
        List<GameVO> games = userGameMapper.listGameVOsByUserId(userId);
        if (games == null) {
            return new ArrayList<>();
        }
        if (games.isEmpty()) {
            return games;
        }
        // 标签与类型各查一次后按 gameId 分组，避免按游戏逐条查询造成 N+1
        Map<Long, List<String>> tagMap = userGameTagMapper.listByUserId(userId).stream()
                .collect(Collectors.groupingBy(UserGameTag::getGameId, LinkedHashMap::new,
                        Collectors.mapping(UserGameTag::getTag, Collectors.toList())));
        Map<Long, List<String>> genreMap = userGameMapper.listGenreRowsByUserId(userId).stream()
                .collect(Collectors.groupingBy(GameGenreRow::getGameId, LinkedHashMap::new,
                        Collectors.mapping(GameGenreRow::getName, Collectors.toList())));
        for (GameVO game : games) {
            game.setTags(tagMap.getOrDefault(game.getId(), new ArrayList<>()));
            game.setGenres(genreMap.getOrDefault(game.getId(), new ArrayList<>()));
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

        // tags = 用户标签（user_game_tags）；genres = 全部类型；platforms / achievementList 照旧
        detail.setTags(listUserTagNames(userId, id));
        detail.setGenres(userGameMapper.listGenreNamesByGameId(id));
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
        String status = gameStatusDTO == null || gameStatusDTO.getStatus() == null
                ? null
                : gameStatusDTO.getStatus().trim();
        // 状态校验集中在 Service：空值、空白与非法值统一按非法参数处理
        if (!GameStatusConstant.isValid(status)) {
            throw new BaseException(MessageConstant.INVALID_GAME_STATUS);
        }
        int rows = userGameMapper.updateStatus(userId, gameId, status);
        if (rows == 0) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }
        return reloadGameVO(gameId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameVO updateTags(Long gameId, GameTagsDTO gameTagsDTO) {
        Long userId = currentUserId();
        // 归属校验优先：非本人游戏一律视为不存在，避免越权探测
        if (userGameMapper.getGameVOByIdAndUserId(gameId, userId) == null) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }
        if (gameTagsDTO == null || gameTagsDTO.getTags() == null) {
            throw new BaseException(MessageConstant.INVALID_TAG);
        }

        List<String> tags = normalizeTags(gameTagsDTO.getTags());

        // 全量替换：先删后插，事务内保证"失败不改原标签"
        userGameTagMapper.deleteByUserIdAndGameId(userId, gameId);
        if (!tags.isEmpty()) {
            List<UserGameTag> rows = new ArrayList<>(tags.size());
            for (String tag : tags) {
                UserGameTag row = new UserGameTag();
                row.setUserId(userId);
                row.setGameId(gameId);
                row.setTag(tag);
                rows.add(row);
            }
            userGameTagMapper.batchInsert(rows);
        }

        return reloadGameVO(gameId, userId);
    }

    @Override
    public List<DailyPlayVO> listPlayTrend(Long gameId, Integer days) {
        Long userId = currentUserId();
        int span = days == null ? DEFAULT_TREND_DAYS : days;
        if (span < MIN_TREND_DAYS || span > MAX_TREND_DAYS) {
            throw new BaseException("参数 days 需在 1-365 之间");
        }
        // 归属校验：非本人游戏视为不存在
        if (userGameMapper.getGameVOByIdAndUserId(gameId, userId) == null) {
            throw new BaseException(MessageConstant.GAME_NOT_FOUND);
        }
        List<DailyPlayVO> trend = userGameMapper.listPlayTrendByUserIdAndGameId(userId, gameId, span - 1);
        return trend == null ? new ArrayList<>() : trend;
    }

    /**
     * 标签归一化：trim、丢弃空标签、去重保序，再校验数量与单标签长度
     */
    private List<String> normalizeTags(List<String> rawTags) {
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : rawTags) {
            if (raw == null) {
                continue;
            }
            String tag = raw.trim();
            if (tag.isEmpty() || !seen.add(tag)) {
                continue;
            }
            normalized.add(tag);
        }
        if (normalized.size() > MAX_TAG_COUNT) {
            throw new BaseException(MessageConstant.TAG_LIMIT_EXCEEDED);
        }
        for (String tag : normalized) {
            if (tag.codePointCount(0, tag.length()) > MAX_TAG_LENGTH) {
                throw new BaseException(MessageConstant.TAG_LIMIT_EXCEEDED);
            }
        }
        return normalized;
    }

    /** 重新查询游戏信息并填充用户标签与全部类型 */
    private GameVO reloadGameVO(Long gameId, Long userId) {
        GameVO gameVO = userGameMapper.getGameVOByIdAndUserId(gameId, userId);
        if (gameVO != null) {
            gameVO.setTags(listUserTagNames(userId, gameId));
            gameVO.setGenres(userGameMapper.listGenreNamesByGameId(gameId));
        }
        return gameVO;
    }

    /** 查询当前用户某款游戏的标签名列表，空时返回空列表 */
    private List<String> listUserTagNames(Long userId, Long gameId) {
        List<UserGameTag> tags = userGameTagMapper.listByUserIdAndGameId(userId, gameId);
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        return tags.stream().map(UserGameTag::getTag).collect(Collectors.toList());
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