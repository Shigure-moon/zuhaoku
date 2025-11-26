package com.zhk.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.entity.Account;
import com.zhk.order.entity.Appeal;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.order.mapper.OrderAccountMapper;
import com.zhk.order.mapper.AppealMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务服务
 * 负责订单到期自动关闭和账号自动回收
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimerService {

    private final LeaseOrderMapper orderMapper;
    private final OrderAccountMapper accountMapper;
    private final AppealMapper appealMapper;
    private final OrderService orderService;

    /**
     * 定时任务：检查并关闭到期的订单
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒 = 1分钟
    public void checkExpiredOrders() {
        try {
            log.debug("开始检查到期订单...");
            
            // 查询所有租赁中且已到期的订单
            LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LeaseOrder::getStatus, "leasing")
                   .le(LeaseOrder::getEndTime, LocalDateTime.now());
            
            List<LeaseOrder> expiredOrders = orderMapper.selectList(wrapper);
            
            if (expiredOrders.isEmpty()) {
                log.debug("没有到期的订单");
                return;
            }
            
            log.info("发现 {} 个到期订单，开始自动关闭", expiredOrders.size());
            
            for (LeaseOrder order : expiredOrders) {
                try {
                    autoCloseOrder(order);
                } catch (Exception e) {
                    log.error("自动关闭订单失败: orderId={}, error={}", order.getId(), e.getMessage(), e);
                }
            }
            
            log.info("订单到期检查完成，处理了 {} 个订单", expiredOrders.size());
        } catch (Exception e) {
            log.error("检查到期订单时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 自动关闭订单
     * 1. 更新订单状态为 closed
     * 2. 检查是否有申诉记录
     * 3. 如果没有申诉记录，更新账号状态为上架（status = 1）
     * 4. 如果有申诉记录，等待申诉处理完成后再处理
     * 5. 记录实际结束时间
     *
     * @param order 订单
     */
    @Transactional
    public void autoCloseOrder(LeaseOrder order) {
        log.info("自动关闭订单: orderId={}, accountId={}", order.getId(), order.getAccountId());
        
        // 更新订单状态
        order.setStatus("closed");
        order.setActualEndTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 检查是否有申诉记录
        LambdaQueryWrapper<Appeal> appealWrapper = new LambdaQueryWrapper<>();
        appealWrapper.eq(Appeal::getOrderId, order.getId());
        Long appealCount = appealMapper.selectCount(appealWrapper);
        boolean hasAppeal = appealCount > 0;
        
        // 更新账号状态：如果没有申诉记录，则上架账号；如果有申诉记录，等待申诉处理完成后再处理
        Account account = accountMapper.selectById(order.getAccountId());
        if (account != null) {
            if (!hasAppeal) {
                // 没有申诉记录，直接上架账号
                account.setStatus(1); // 1 = 上架
                accountMapper.updateById(account);
                log.info("订单自动关闭，账号已自动上架: orderId={}, accountId={}", order.getId(), account.getId());
            } else {
                // 有申诉记录，保持账号状态不变，等待申诉处理完成后再处理
                log.info("订单自动关闭，但存在申诉记录，账号状态暂不更新: orderId={}, accountId={}", order.getId(), account.getId());
            }
        } else {
            log.warn("订单关联的账号不存在: orderId={}, accountId={}", order.getId(), order.getAccountId());
        }
    }

    /**
     * 定时任务：检查即将到期的订单（提前5分钟提醒）
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void checkExpiringOrders() {
        try {
            // 查询所有租赁中且即将在5分钟内到期的订单
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderTime = now.plusMinutes(5);
            
            LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LeaseOrder::getStatus, "leasing")
                   .ge(LeaseOrder::getEndTime, now)
                   .le(LeaseOrder::getEndTime, reminderTime);
            
            List<LeaseOrder> expiringOrders = orderMapper.selectList(wrapper);
            
            if (!expiringOrders.isEmpty()) {
                log.info("发现 {} 个即将到期的订单（5分钟内）", expiringOrders.size());
                // TODO: 发送提醒通知（WebSocket推送、短信等）
                for (LeaseOrder order : expiringOrders) {
                    log.debug("订单即将到期: orderId={}, endTime={}", order.getId(), order.getEndTime());
                }
            }
        } catch (Exception e) {
            log.error("检查即将到期订单时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时任务：清理超时未支付的订单
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupUnpaidOrders() {
        try {
            log.debug("开始清理超时未支付的订单...");
            
            // 查询所有支付中且创建时间超过30分钟的订单
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
            
            LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LeaseOrder::getStatus, "paying")
                   .le(LeaseOrder::getCreatedAt, expireTime);
            
            List<LeaseOrder> unpaidOrders = orderMapper.selectList(wrapper);
            
            if (unpaidOrders.isEmpty()) {
                log.debug("没有超时未支付的订单");
                return;
            }
            
            log.info("发现 {} 个超时未支付的订单，开始自动取消", unpaidOrders.size());
            
            for (LeaseOrder order : unpaidOrders) {
                try {
                    // 取消订单
                    orderService.cancelOrder(order.getId(), order.getTenantUid());
                    log.info("已自动取消超时未支付订单: orderId={}", order.getId());
                } catch (Exception e) {
                    log.error("自动取消订单失败: orderId={}, error={}", order.getId(), e.getMessage(), e);
                }
            }
            
            log.info("超时未支付订单清理完成，处理了 {} 个订单", unpaidOrders.size());
        } catch (Exception e) {
            log.error("清理超时未支付订单时发生异常: {}", e.getMessage(), e);
        }
    }
}

