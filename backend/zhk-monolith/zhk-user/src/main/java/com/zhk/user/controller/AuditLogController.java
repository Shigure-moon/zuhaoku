package com.zhk.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.common.security.SecurityUtils;
import com.zhk.common.web.BusinessException;
import com.zhk.common.web.Result;
import com.zhk.user.dto.AuditLogQueryDTO;
import com.zhk.user.dto.AuditLogVO;
import com.zhk.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 日志审计控制器
 *
 * @author shigure
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 查询日志列表
     */
    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<Page<AuditLogVO>> queryLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        AuditLogQueryDTO queryDTO = new AuditLogQueryDTO();
        queryDTO.setUserId(userId);
        queryDTO.setUsername(username);
        queryDTO.setRole(role);
        queryDTO.setAction(action);
        queryDTO.setResourceType(resourceType);
        queryDTO.setResourceId(resourceId);
        queryDTO.setSuccess(success);
        queryDTO.setPage(page);
        queryDTO.setPageSize(pageSize);

        // 解析时间字符串（简单实现，实际可以使用更完善的日期解析）
        if (startTime != null && !startTime.isEmpty()) {
            try {
                queryDTO.setStartTime(java.time.LocalDateTime.parse(startTime));
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        if (endTime != null && !endTime.isEmpty()) {
            try {
                queryDTO.setEndTime(java.time.LocalDateTime.parse(endTime));
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        try {
            log.info("查询日志列表: page={}, pageSize={}, userId={}, action={}", 
                    page, pageSize, userId, action);
            Page<AuditLogVO> result = auditLogService.queryLogs(queryDTO);
            log.info("查询日志列表成功: total={}, records={}", 
                    result.getTotal(), result.getRecords() != null ? result.getRecords().size() : 0);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询日志列表失败", e);
            e.printStackTrace();
            throw new BusinessException(500, "查询日志列表失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}

