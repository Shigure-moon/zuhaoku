package com.zhk.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhk.common.web.Result;
import com.zhk.user.entity.Game;
import com.zhk.user.mapper.GameMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏控制器
 *
 * @author shigure
 */
@Slf4j
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
        try {
            log.info("开始查询游戏列表");
            LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Game::getStatus, 1); // 只返回启用的游戏
            wrapper.orderByAsc(Game::getId);
            List<Game> games = gameMapper.selectList(wrapper);
            log.info("查询游戏列表成功，数量: {}", games != null ? games.size() : 0);
            return Result.success(games != null ? games : new ArrayList<>());
        } catch (Exception e) {
            log.error("查询游戏列表失败", e);
            // 返回空列表而不是抛出异常，避免应用崩溃
            return Result.success(new ArrayList<>());
        }
    }
}

