package com.zhk.user.service;

import com.zhk.user.dto.AccountVO;
import com.zhk.user.dto.CreateAccountDTO;
import com.zhk.user.dto.UpdateAccountDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 账号服务接口
 *
 * @author shigure
 */
public interface AccountService {

    /**
     * 获取账号列表（公开接口）
     *
     * @param gameId     游戏ID
     * @param keyword    关键词
     * @param minPrice   最低价格
     * @param maxPrice   最高价格
     * @param status     状态
     * @param page       页码
     * @param pageSize   每页数量
     * @param sortBy     排序字段
     * @param sortOrder  排序顺序
     * @return 账号列表
     */
    Map<String, Object> getAccountList(
            Integer gameId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String status,
            Integer page,
            Integer pageSize,
            String sortBy,
            String sortOrder
    );

    /**
     * 获取我的账号列表（商家端）
     *
     * @param userId   用户ID
     * @param gameId   游戏ID
     * @param status   状态
     * @param page     页码
     * @param pageSize 每页数量
     * @return 账号列表
     */
    Map<String, Object> getMyAccounts(
            Long userId,
            Integer gameId,
            String status,
            Integer page,
            Integer pageSize
    );

    /**
     * 创建账号
     *
     * @param userId 用户ID
     * @param dto    创建账号DTO
     * @return 账号VO
     */
    AccountVO createAccount(Long userId, CreateAccountDTO dto);

    /**
     * 获取账号详情
     *
     * @param id 账号ID
     * @return 账号VO
     */
    AccountVO getAccountDetail(Long id);

    /**
     * 更新账号
     *
     * @param userId 用户ID
     * @param id     账号ID
     * @param dto    更新账号DTO
     * @return 账号VO
     */
    AccountVO updateAccount(Long userId, Long id, UpdateAccountDTO dto);

    /**
     * 删除账号
     *
     * @param userId 用户ID
     * @param id     账号ID
     */
    void deleteAccount(Long userId, Long id);

    /**
     * 上架账号
     *
     * @param userId 用户ID
     * @param id     账号ID
     * @return 账号VO
     */
    AccountVO onlineAccount(Long userId, Long id);

    /**
     * 下架账号
     *
     * @param userId 用户ID
     * @param id     账号ID
     * @return 账号VO
     */
    AccountVO offlineAccount(Long userId, Long id);

    /**
     * 获取账号的明文密码（仅限号主）
     *
     * @param userId 用户ID
     * @param id     账号ID
     * @return 账号密码
     */
    String getAccountPassword(Long userId, Long id);
}

