package com.zhk.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.user.dto.AuditLogQueryDTO;
import com.zhk.user.dto.AuditLogVO;
import com.zhk.user.entity.AuditLog;
import com.zhk.user.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志审计服务
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 记录日志
     */
    public void log(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 查询日志列表
     */
    public Page<AuditLogVO> queryLogs(AuditLogQueryDTO queryDTO) {
        try {
            log.debug("开始查询日志列表: {}", queryDTO);
            LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
            
            if (queryDTO.getUserId() != null) {
                wrapper.eq(AuditLog::getUserId, queryDTO.getUserId());
            }
            if (queryDTO.getUsername() != null && !queryDTO.getUsername().isEmpty()) {
                wrapper.like(AuditLog::getUsername, queryDTO.getUsername());
            }
            if (queryDTO.getRole() != null && !queryDTO.getRole().isEmpty()) {
                wrapper.eq(AuditLog::getRole, queryDTO.getRole());
            }
            if (queryDTO.getAction() != null && !queryDTO.getAction().isEmpty()) {
                wrapper.eq(AuditLog::getAction, queryDTO.getAction());
            }
            if (queryDTO.getResourceType() != null && !queryDTO.getResourceType().isEmpty()) {
                wrapper.eq(AuditLog::getResourceType, queryDTO.getResourceType());
            }
            if (queryDTO.getResourceId() != null) {
                wrapper.eq(AuditLog::getResourceId, queryDTO.getResourceId());
            }
            if (queryDTO.getSuccess() != null) {
                wrapper.eq(AuditLog::getSuccess, queryDTO.getSuccess());
            }
            if (queryDTO.getStartTime() != null) {
                wrapper.ge(AuditLog::getCreatedAt, queryDTO.getStartTime());
            }
            if (queryDTO.getEndTime() != null) {
                wrapper.le(AuditLog::getCreatedAt, queryDTO.getEndTime());
            }
            
            wrapper.orderByDesc(AuditLog::getCreatedAt);
            
            Page<AuditLog> page = new Page<>(queryDTO.getPage() != null ? queryDTO.getPage() : 1, 
                                            queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 20);
            log.debug("执行分页查询: page={}, pageSize={}", page.getCurrent(), page.getSize());
            
            Page<AuditLog> result = auditLogMapper.selectPage(page, wrapper);
            log.debug("查询结果: total={}, records={}", result.getTotal(), 
                     result.getRecords() != null ? result.getRecords().size() : 0);
            
            // 转换为VO
            Page<AuditLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
            List<AuditLogVO> voList = result.getRecords() != null ? 
                    result.getRecords().stream()
                            .map(this::convertToVO)
                            .collect(Collectors.toList()) : 
                    new java.util.ArrayList<>();
            voPage.setRecords(voList);
            
            return voPage;
        } catch (Exception e) {
            log.error("查询日志列表失败", e);
            e.printStackTrace();
            throw new RuntimeException("查询日志列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换为VO
     */
    private AuditLogVO convertToVO(AuditLog auditLog) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(auditLog, vo);
        return vo;
    }
}

