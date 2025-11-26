package com.zhk.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhk.common.web.BusinessException;
import com.zhk.order.dto.AppealVO;
import com.zhk.order.dto.CreateAppealDTO;
import com.zhk.order.dto.ResolveAppealDTO;
import com.zhk.order.entity.Appeal;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.mapper.AppealMapper;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.order.service.AppealService;
import com.zhk.order.entity.Account;
import com.zhk.order.entity.User;
import com.zhk.order.mapper.OrderAccountMapper;
import com.zhk.order.mapper.OrderUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 申诉服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    private final AppealMapper appealMapper;
    private final LeaseOrderMapper orderMapper;
    private final OrderAccountMapper accountMapper;
    private final OrderUserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public AppealVO createAppeal(Long userId, CreateAppealDTO dto) {
        // 查询订单
        LeaseOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 验证权限（租客或号主可以申诉）
        Account account = accountMapper.selectById(order.getAccountId());
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }

        if (!order.getTenantUid().equals(userId) && !account.getOwnerUid().equals(userId)) {
            throw new BusinessException(403, "无权限创建此申诉");
        }

        // 检查是否已有申诉
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appeal::getOrderId, dto.getOrderId())
               .isNull(Appeal::getVerdict);
        Appeal existingAppeal = appealMapper.selectOne(wrapper);
        if (existingAppeal != null) {
            throw new BusinessException(400, "该订单已有待处理的申诉");
        }

        // 更新订单状态为申诉中
        order.setStatus("appeal");
        orderMapper.updateById(order);

        // 创建申诉
        Appeal appeal = new Appeal();
        appeal.setOrderId(dto.getOrderId());
        appeal.setType(dto.getType());
        try {
            appeal.setEvidenceUrls(objectMapper.writeValueAsString(dto.getEvidenceUrls()));
        } catch (Exception e) {
            log.error("序列化证据URL失败", e);
        }

        appealMapper.insert(appeal);

        return convertToVO(appeal);
    }

    @Override
    public List<AppealVO> getAppealList(String status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        
        // 状态筛选（verdict为null表示待处理）
        if ("pending".equals(status)) {
            wrapper.isNull(Appeal::getVerdict);
        } else if ("resolved".equals(status)) {
            wrapper.isNotNull(Appeal::getVerdict);
        }

        wrapper.orderByDesc(Appeal::getCreateTime);

        Page<Appeal> pageParam = new Page<>(page, pageSize);
        Page<Appeal> appealPage = appealMapper.selectPage(pageParam, wrapper);

        List<AppealVO> voList = new ArrayList<>();
        for (Appeal appeal : appealPage.getRecords()) {
            voList.add(convertToVO(appeal));
        }

        return voList;
    }

    @Override
    public Long getAppealCount(String status) {
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        
        if ("pending".equals(status)) {
            wrapper.isNull(Appeal::getVerdict);
        } else if ("resolved".equals(status)) {
            wrapper.isNotNull(Appeal::getVerdict);
        }

        return appealMapper.selectCount(wrapper);
    }

    @Override
    public AppealVO getAppealDetail(Long appealId) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(404, "申诉不存在");
        }

        return convertToVO(appeal);
    }

    @Override
    @Transactional
    public AppealVO resolveAppeal(Long appealId, Long operatorId, ResolveAppealDTO dto) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(404, "申诉不存在");
        }

        if (appeal.getVerdict() != null) {
            throw new BusinessException(400, "申诉已处理");
        }

        // 更新申诉 - 使用 LambdaUpdateWrapper 确保字段正确更新
        LocalDateTime resolveTime = LocalDateTime.now();
        LambdaUpdateWrapper<Appeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Appeal::getId, appealId)
                .set(Appeal::getVerdict, dto.getVerdict())
                .set(Appeal::getOperatorUid, operatorId)
                .set(Appeal::getResolveTime, resolveTime);
        
        int updateCount = appealMapper.update(null, updateWrapper);
        log.info("更新申诉状态: appealId={}, verdict={}, operatorId={}, resolveTime={}, updateCount={}", 
                appealId, dto.getVerdict(), operatorId, resolveTime, updateCount);
        
        if (updateCount == 0) {
            log.error("更新申诉失败: appealId={}, verdict={}, updateCount=0", appealId, dto.getVerdict());
            throw new BusinessException(500, "更新申诉状态失败");
        }

        // 更新订单状态
        LeaseOrder order = orderMapper.selectById(appeal.getOrderId());
        if (order != null) {
            // 根据裁决结果处理订单
            // TODO: 实现具体的退款、分账逻辑
            order.setStatus("closed");
            int orderUpdateCount = orderMapper.updateById(order);
            log.info("更新订单状态: orderId={}, status=closed, updateCount={}", 
                    appeal.getOrderId(), orderUpdateCount);
            
            // 申诉处理完成后，下架账号
            Account account = accountMapper.selectById(order.getAccountId());
            if (account != null) {
                account.setStatus(2); // 2 = 下架
                int accountUpdateCount = accountMapper.updateById(account);
                log.info("申诉处理完成，账号已下架: orderId={}, accountId={}, updateCount={}", 
                        appeal.getOrderId(), account.getId(), accountUpdateCount);
            } else {
                log.warn("订单关联的账号不存在: orderId={}, accountId={}", 
                        appeal.getOrderId(), order.getAccountId());
            }
        }

        // 重新查询以确保获取最新数据
        Appeal updatedAppeal = appealMapper.selectById(appealId);
        if (updatedAppeal == null) {
            log.error("申诉不存在: appealId={}", appealId);
            throw new BusinessException(404, "申诉不存在");
        }
        
        log.info("申诉处理完成: appealId={}, verdict={}, resolveTime={}, operatorUid={}", 
                appealId, updatedAppeal.getVerdict(), updatedAppeal.getResolveTime(), updatedAppeal.getOperatorUid());
        
        // 验证更新是否成功
        if (updatedAppeal.getVerdict() == null) {
            log.error("申诉更新失败: appealId={}, verdict仍为null", appealId);
            throw new BusinessException(500, "申诉更新失败，请重试");
        }

        return convertToVO(updatedAppeal);
    }

    /**
     * 转换为 VO
     */
    private AppealVO convertToVO(Appeal appeal) {
        AppealVO vo = new AppealVO();
        BeanUtils.copyProperties(appeal, vo);

        // 设置类型名称
        if (appeal.getType() == 1) {
            vo.setTypeName("账号异常");
        } else if (appeal.getType() == 2) {
            vo.setTypeName("押金争议");
        } else if (appeal.getType() == 4) {
            vo.setTypeName("玩家恶意使用/销毁资源");
        } else if (appeal.getType() == 5) {
            vo.setTypeName("买家脚本盗号");
        } else {
            vo.setTypeName("其他");
        }

        // 设置裁决结果名称
        if (appeal.getVerdict() != null) {
            if (appeal.getVerdict() == 1) {
                vo.setVerdictName("支持租客");
            } else if (appeal.getVerdict() == 2) {
                vo.setVerdictName("支持号主");
            } else {
                vo.setVerdictName("各担一半");
            }
        }

        // 解析证据URL
        if (appeal.getEvidenceUrls() != null) {
            try {
                List<String> urls = objectMapper.readValue(appeal.getEvidenceUrls(), new TypeReference<List<String>>() {});
                vo.setEvidenceUrls(urls);
            } catch (Exception e) {
                log.error("解析证据URL失败", e);
            }
        }

        // 查询处理人信息
        if (appeal.getOperatorUid() != null) {
            User operator = userMapper.selectById(appeal.getOperatorUid());
            vo.setOperatorName(operator != null ? operator.getNickname() : "");
        }

        return vo;
    }
}

