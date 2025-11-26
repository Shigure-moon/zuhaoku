package com.zhk.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhk.common.web.Result;
import com.zhk.user.entity.Game;
import com.zhk.user.mapper.GameMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 游戏控制器
 *
 * @author shigure
 */
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameMapper gameMapper;

    /**
     * 获取游戏列表
     */
    @GetMapping
    public Result<List<Game>> getGameList() {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Game::getStatus, 1); // 只返回启用的游戏
        wrapper.orderByAsc(Game::getId);
        List<Game> games = gameMapper.selectList(wrapper);
        return Result.success(games);
    }
}

