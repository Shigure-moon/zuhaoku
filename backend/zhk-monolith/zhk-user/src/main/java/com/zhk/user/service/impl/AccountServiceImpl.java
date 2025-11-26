package com.zhk.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.common.web.BusinessException;
import com.zhk.user.dto.AccountVO;
import com.zhk.user.dto.CreateAccountDTO;
import com.zhk.user.dto.UpdateAccountDTO;
import com.zhk.user.entity.Account;
import com.zhk.user.entity.Game;
import com.zhk.user.mapper.AccountMapper;
import com.zhk.user.mapper.GameMapper;
import com.zhk.user.service.AccountService;
import com.zhk.user.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账号服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final GameMapper gameMapper;
    private final EncryptionService encryptionService;

    @Override
    public Map<String, Object> getAccountList(
            Integer gameId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String status,
            Integer page,
            Integer pageSize,
            String sortBy,
            String sortOrder
    ) {
        // 构建查询条件
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询上架的账号
        wrapper.eq(Account::getStatus, 1);
        
        // 游戏ID筛选
        if (gameId != null) {
            wrapper.eq(Account::getGameId, gameId);
        }
        
        // 关键词搜索（标题、描述）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Account::getTitle, keyword)
                    .or()
                    .like(Account::getDescription, keyword));
        }
        
        // 价格筛选
        if (minPrice != null) {
            wrapper.ge(Account::getPrice1h, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Account::getPrice1h, maxPrice);
        }
        
        // 排序
        if (StringUtils.hasText(sortBy)) {
            if ("price".equals(sortBy)) {
                if ("desc".equalsIgnoreCase(sortOrder)) {
                    wrapper.orderByDesc(Account::getPrice1h);
                } else {
                    wrapper.orderByAsc(Account::getPrice1h);
                }
            } else if ("createdAt".equals(sortBy)) {
                if ("desc".equalsIgnoreCase(sortOrder)) {
                    wrapper.orderByDesc(Account::getCreatedAt);
                } else {
                    wrapper.orderByAsc(Account::getCreatedAt);
                }
            }
        } else {
            // 默认按创建时间倒序
            wrapper.orderByDesc(Account::getCreatedAt);
        }
        
        // 分页查询
        Page<Account> pageParam = new Page<>(page, pageSize);
        Page<Account> accountPage = accountMapper.selectPage(pageParam, wrapper);
        
        // 获取游戏信息映射
        List<Game> games = gameMapper.selectList(null);
        Map<Integer, String> gameMap = games.stream()
                .collect(Collectors.toMap(Game::getId, Game::getName));
        
        // 转换为 VO
        List<AccountVO> voList = accountPage.getRecords().stream()
                .map(account -> convertToVO(account, gameMap))
                .collect(Collectors.toList());
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", accountPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return result;
    }

    @Override
    public Map<String, Object> getMyAccounts(
            Long userId,
            Integer gameId,
            String status,
            Integer page,
            Integer pageSize
    ) {
        // 构建查询条件
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getOwnerUid, userId);
        
        // 游戏ID筛选
        if (gameId != null) {
            wrapper.eq(Account::getGameId, gameId);
        }
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            if ("ONLINE".equals(status)) {
                wrapper.eq(Account::getStatus, 1);
            } else if ("OFFLINE".equals(status)) {
                wrapper.eq(Account::getStatus, 2);
            } else if ("RENTED".equals(status)) {
                wrapper.eq(Account::getStatus, 3);
            }
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Account::getCreatedAt);
        
        // 分页查询
        Page<Account> pageParam = new Page<>(page, pageSize);
        Page<Account> accountPage = accountMapper.selectPage(pageParam, wrapper);
        
        // 获取游戏信息映射
        List<Game> games = gameMapper.selectList(null);
        Map<Integer, String> gameMap = games.stream()
                .collect(Collectors.toMap(Game::getId, Game::getName));
        
        // 转换为 VO
        List<AccountVO> voList = accountPage.getRecords().stream()
                .map(account -> convertToVO(account, gameMap))
                .collect(Collectors.toList());
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", accountPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return result;
    }

    @Override
    @Transactional
    public AccountVO createAccount(Long userId, CreateAccountDTO dto) {
        log.info("创建账号: userId={}, gameId={}, title={}", userId, dto.getGameId(), dto.getTitle());
        
        // 验证游戏是否存在
        Game game = gameMapper.selectById(dto.getGameId());
        if (game == null) {
            throw new BusinessException(404, "游戏不存在");
        }
        
        // 创建账号实体
        Account account = new Account();
        account.setGameId(dto.getGameId());
        account.setOwnerUid(userId);
        account.setTitle(dto.getTitle());
        account.setDescription(dto.getDescription());
        if (dto.getLevel() != null) {
            account.setLvl(dto.getLevel());
        }
        
        // 设置价格
        account.setPrice1h(dto.getPricePerHour());
        if (dto.getPricePerNight() != null) {
            account.setPriceOvernight(dto.getPricePerNight());
        } else {
            // 默认包夜价格为时租价格的8倍
            account.setPriceOvernight(dto.getPricePerHour().multiply(new BigDecimal("8")));
        }
        // 30分钟价格为时租价格的一半
        account.setPrice30min(dto.getPricePerHour().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
        
        // 设置押金（默认为时租价格的10倍）
        account.setDeposit(dto.getPricePerHour().multiply(new BigDecimal("10")));
        
        // 状态：默认上架
        account.setStatus(1);
        
        // 先保存账号以获取ID（用于加密）
        account.setUsernameEnc(""); // 临时值
        account.setPwdEnc(""); // 临时值
        account.setIv(""); // 临时值
        accountMapper.insert(account);
        
        // 使用账号ID进行加密
        try {
            account.setUsernameEnc(encryptionService.encrypt(dto.getUsername(), account.getId()));
            account.setPwdEnc(encryptionService.encrypt(dto.getPassword(), account.getId()));
            account.setIv(encryptionService.generateIV());
        } catch (Exception e) {
            log.error("账号加密失败: accountId={}, error={}", account.getId(), e.getMessage(), e);
            throw new BusinessException(500, "账号加密失败: " + e.getMessage());
        }
        
        // 更新账号（保存加密后的数据）
        accountMapper.updateById(account);
        
        log.info("账号创建成功: accountId={}, userId={}", account.getId(), userId);
        
        return convertToVO(account, game);
    }

    @Override
    public AccountVO getAccountDetail(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }
        
        Game game = gameMapper.selectById(account.getGameId());
        return convertToVO(account, game);
    }

    @Override
    @Transactional
    public AccountVO updateAccount(Long userId, Long id, UpdateAccountDTO dto) {
        log.info("更新账号: userId={}, accountId={}", userId, id);
        
        // 查询账号
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }
        
        // 验证是否为账号所有者
        if (!account.getOwnerUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此账号");
        }
        
        // 检查账号是否在租赁中
        if (account.getStatus() == 3) {
            throw new BusinessException(400, "账号正在租赁中，无法修改");
        }
        
        // 更新账号信息
        boolean updated = false;
        if (dto.getTitle() != null) {
            account.setTitle(dto.getTitle());
            updated = true;
        }
        if (dto.getDescription() != null) {
            account.setDescription(dto.getDescription());
            updated = true;
        }
        if (dto.getPricePerHour() != null) {
            account.setPrice1h(dto.getPricePerHour());
            account.setPrice30min(dto.getPricePerHour().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
            if (dto.getPricePerNight() != null) {
                account.setPriceOvernight(dto.getPricePerNight());
            } else {
                account.setPriceOvernight(dto.getPricePerHour().multiply(new BigDecimal("8")));
            }
            account.setDeposit(dto.getPricePerHour().multiply(new BigDecimal("10")));
            updated = true;
        }
        if (dto.getLevel() != null) {
            account.setLvl(dto.getLevel());
            updated = true;
        }
        
        if (updated) {
            accountMapper.updateById(account);
            log.info("账号更新成功: accountId={}", id);
        }
        
        Game game = gameMapper.selectById(account.getGameId());
        return convertToVO(account, game);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, Long id) {
        log.info("删除账号: userId={}, accountId={}", userId, id);
        
        // 查询账号
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }
        
        // 验证是否为账号所有者
        if (!account.getOwnerUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此账号");
        }
        
        // 检查账号是否在租赁中
        if (account.getStatus() == 3) {
            throw new BusinessException(400, "账号正在租赁中，无法删除");
        }
        
        accountMapper.deleteById(id);
        log.info("账号删除成功: accountId={}", id);
    }

    @Override
    @Transactional
    public AccountVO onlineAccount(Long userId, Long id) {
        log.info("上架账号: userId={}, accountId={}", userId, id);
        
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }
        
        if (!account.getOwnerUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此账号");
        }
        
        if (account.getStatus() == 3) {
            throw new BusinessException(400, "账号正在租赁中，无法上架");
        }
        
        account.setStatus(1);
        accountMapper.updateById(account);
        
        log.info("账号上架成功: accountId={}", id);
        
        Game game = gameMapper.selectById(account.getGameId());
        return convertToVO(account, game);
    }

    @Override
    @Transactional
    public AccountVO offlineAccount(Long userId, Long id) {
        log.info("下架账号: userId={}, accountId={}", id);
        
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }
        
        if (!account.getOwnerUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此账号");
        }
        
        if (account.getStatus() == 3) {
            throw new BusinessException(400, "账号正在租赁中，无法下架");
        }
        
        account.setStatus(2);
        accountMapper.updateById(account);
        
        log.info("账号下架成功: accountId={}", id);
        
        Game game = gameMapper.selectById(account.getGameId());
        return convertToVO(account, game);
    }

    @Override
    public String getAccountPassword(Long userId, Long id) {
        log.info("获取账号密码: userId={}, accountId={}", userId, id);
        
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }
        
        // 验证是否为账号所有者
        if (!account.getOwnerUid().equals(userId)) {
            throw new BusinessException(403, "无权限查看此账号密码");
        }
        
        try {
            return encryptionService.decrypt(account.getPwdEnc(), account.getId());
        } catch (Exception e) {
            log.error("账号密码解密失败: accountId={}, error={}", id, e.getMessage(), e);
            throw new BusinessException(500, "密码解密失败: " + e.getMessage());
        }
    }

    /**
     * 转换为 VO
     */
    private AccountVO convertToVO(Account account, Game game) {
        AccountVO vo = new AccountVO();
        vo.setId(account.getId());
        vo.setGameId(account.getGameId());
        vo.setGameName(game != null ? game.getName() : "");
        vo.setOwnerId(account.getOwnerUid());
        vo.setTitle(account.getTitle() != null && !account.getTitle().isEmpty() 
                ? account.getTitle() 
                : (game != null ? game.getName() + " 账号 - Lv." + (account.getLvl() != null ? account.getLvl() : "?") : ""));
        vo.setDescription(account.getDescription());
        vo.setPricePerHour(account.getPrice1h());
        vo.setPricePerNight(account.getPriceOvernight());
        vo.setDeposit(account.getDeposit());
        
        // 状态转换
        if (account.getStatus() == 1) {
            vo.setStatus("ONLINE");
        } else if (account.getStatus() == 2) {
            vo.setStatus("OFFLINE");
        } else if (account.getStatus() == 3) {
            vo.setStatus("RENTED");
        }
        
        vo.setLevel(account.getLvl());
        vo.setCreatedAt(account.getCreatedAt());
        vo.setUpdatedAt(account.getUpdatedAt());
        
        return vo;
    }

    /**
     * 转换为 VO（使用游戏映射）
     */
    private AccountVO convertToVO(Account account, Map<Integer, String> gameMap) {
        String gameName = gameMap.getOrDefault(account.getGameId(), "");
        Game game = gameName.isEmpty() ? null : new Game();
        if (game != null) {
            game.setId(account.getGameId());
            game.setName(gameName);
        }
        return convertToVO(account, game);
    }
}

