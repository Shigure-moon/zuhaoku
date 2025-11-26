package com.zhk.order.service;

import com.zhk.order.dto.AppealVO;
import com.zhk.order.dto.CreateAppealDTO;
import com.zhk.order.dto.ResolveAppealDTO;

import java.util.List;

/**
 * 申诉服务接口
 *
 * @author shigure
 */
public interface AppealService {
    /**
     * 创建申诉
     */
    AppealVO createAppeal(Long userId, CreateAppealDTO dto);

    /**
     * 查询申诉列表
     */
    List<AppealVO> getAppealList(String status, Integer page, Integer pageSize);

    /**
     * 获取申诉总数
     */
    Long getAppealCount(String status);

    /**
     * 获取申诉详情
     */
    AppealVO getAppealDetail(Long appealId);

    /**
     * 处理申诉（管理员）
     */
    AppealVO resolveAppeal(Long appealId, Long operatorId, ResolveAppealDTO dto);
}

