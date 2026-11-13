package com.lxw.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 游戏状态常量（user_games.status 唯一取值来源）
 * <p>
 * 与 {@link StatusConstant}（启用/禁用）无关，游戏状态只允许中文五值：
 * 在玩 / 已完成 / 未玩 / 搁置 / 弃坑。
 */
public class GameStatusConstant {

    /** 在玩 */
    public static final String IN_PLAY = "在玩";

    /** 已完成 */
    public static final String FINISHED = "已完成";

    /** 未玩 */
    public static final String UNPLAYED = "未玩";

    /** 搁置 */
    public static final String SHELVED = "搁置";

    /** 弃坑 */
    public static final String ABANDONED = "弃坑";

    /** 全部合法游戏状态 */
    public static final List<String> ALL = Collections.unmodifiableList(
            Arrays.asList(IN_PLAY, FINISHED, UNPLAYED, SHELVED, ABANDONED));

    /**
     * 判断状态值是否在中文五值白名单内（不负责 trim，调用方需先 trim）
     *
     * @param status 游戏状态
     * @return 合法返回 true，null 或非法值返回 false
     */
    public static boolean isValid(String status) {
        return status != null && ALL.contains(status);
    }
}