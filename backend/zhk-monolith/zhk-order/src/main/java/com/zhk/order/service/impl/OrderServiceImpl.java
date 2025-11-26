package com.zhk.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.common.web.BusinessException;
import com.zhk.order.dto.CreateOrderDTO;
import com.zhk.order.dto.OrderVO;
import com.zhk.order.dto.RenewOrderDTO;
import com.zhk.order.entity.LeaseOrder;
import com.zhk.order.mapper.LeaseOrderMapper;
import com.zhk.order.service.OrderService;
import com.zhk.order.util.OrderStatusConverter;
import com.zhk.order.entity.Account;
import com.zhk.order.entity.Game;
import com.zhk.order.entity.User;
import com.zhk.order.mapper.OrderAccountMapper;
import com.zhk.order.mapper.OrderGameMapper;
import com.zhk.order.mapper.OrderUserMapper;
import com.zhk.order.mapper.PaymentRecordMapper;
import com.zhk.order.mapper.AppealMapper;
import com.zhk.order.entity.PaymentRecord;
import com.zhk.order.entity.Appeal;
import com.zhk.order.service.EncryptionService;
import com.zhk.order.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 订单服务实现类
 *
 * @author shigure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final LeaseOrderMapper orderMapper;
    private final OrderAccountMapper accountMapper;
    private final OrderGameMapper gameMapper;
    private final OrderUserMapper userMapper;
    private final AppealMapper appealMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final EncryptionService encryptionService;
    private final DistributedLock distributedLock;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderDTO dto) {
        // 使用分布式锁防止重复下单
        String lockKey = "order:create:" + dto.getAccountId() + ":" + userId;
        
        return distributedLock.executeWithLock(lockKey, 3, 10, () -> {
            // 查询账号
            Account account = accountMapper.selectById(dto.getAccountId());
            if (account == null) {
                throw new BusinessException(404, "账号不存在");
            }

            // 检查账号状态
            if (account.getStatus() != 1) {
                throw new BusinessException(400, "账号不可租");
            }

            // 检查是否为账号所有者
            if (account.getOwnerUid().equals(userId)) {
                throw new BusinessException(400, "不能租赁自己的账号");
            }

            // 检查是否已有进行中的订单（防止重复下单）
            LambdaQueryWrapper<LeaseOrder> existingOrderWrapper = new LambdaQueryWrapper<>();
            existingOrderWrapper.eq(LeaseOrder::getAccountId, dto.getAccountId())
                    .eq(LeaseOrder::getTenantUid, userId)
                    .in(LeaseOrder::getStatus, List.of("paying", "leasing"));
            LeaseOrder existingOrder = orderMapper.selectOne(existingOrderWrapper);
            if (existingOrder != null) {
                throw new BusinessException(400, "您已有该账号的进行中订单，请勿重复下单");
            }

            // 计算租期和价格
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime;
            BigDecimal amount;
            BigDecimal deposit = account.getDeposit();

            if ("MINUTE".equals(dto.getDurationType())) {
                endTime = now.plusMinutes(dto.getDuration());
                amount = account.getPrice30min().multiply(new BigDecimal(dto.getDuration()).divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP));
            } else if ("HOUR".equals(dto.getDurationType())) {
                endTime = now.plusHours(dto.getDuration());
                amount = account.getPrice1h().multiply(new BigDecimal(dto.getDuration()));
            } else if ("OVERNIGHT".equals(dto.getDurationType())) {
                endTime = now.plusDays(1).withHour(8).withMinute(0).withSecond(0);
                amount = account.getPriceOvernight();
            } else {
                throw new BusinessException(400, "无效的租期类型");
            }

            // 创建订单
            LeaseOrder order = new LeaseOrder();
            order.setAccountId(dto.getAccountId());
            order.setTenantUid(userId);
            order.setStartTime(now);
            order.setEndTime(endTime);
            order.setAmount(amount);
            order.setDeposit(deposit);
            order.setStatus("paying");

            orderMapper.insert(order);

            // 注意：创建订单时账号状态暂时不变，等支付完成后再改为租赁中
            // 这样可以避免未支付订单占用账号资源

            // 转换为 VO
            return convertToVO(order);
        });
    }

    @Override
    public List<OrderVO> getOrderList(Long userId, String role, String status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
        
        // 根据角色筛选
        if ("TENANT".equals(role)) {
            wrapper.eq(LeaseOrder::getTenantUid, userId);
        } else if ("OWNER".equals(role)) {
            // 需要关联账号表查询
            List<Account> accounts = accountMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                    .eq(Account::getOwnerUid, userId)
            );
            if (accounts.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> accountIds = accounts.stream().map(Account::getId).toList();
            wrapper.in(LeaseOrder::getAccountId, accountIds);
        }

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(LeaseOrder::getStatus, status);
        }

        // 排序
        wrapper.orderByDesc(LeaseOrder::getCreatedAt);

        // 分页查询
        Page<LeaseOrder> pageParam = new Page<>(page, pageSize);
        Page<LeaseOrder> orderPage = orderMapper.selectPage(pageParam, wrapper);

        // 转换为 VO
        List<OrderVO> voList = new ArrayList<>();
        for (LeaseOrder order : orderPage.getRecords()) {
            voList.add(convertToVO(order));
        }

        return voList;
    }

    @Override
    public Long getOrderCount(Long userId, String role, String status) {
        LambdaQueryWrapper<LeaseOrder> wrapper = new LambdaQueryWrapper<>();
        
        if ("TENANT".equals(role)) {
            wrapper.eq(LeaseOrder::getTenantUid, userId);
        } else if ("OWNER".equals(role)) {
            List<Account> accounts = accountMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                    .eq(Account::getOwnerUid, userId)
            );
            if (accounts.isEmpty()) {
                return 0L;
            }
            List<Long> accountIds = accounts.stream().map(Account::getId).toList();
            wrapper.in(LeaseOrder::getAccountId, accountIds);
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq(LeaseOrder::getStatus, status);
        }

        return orderMapper.selectCount(wrapper);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId, Long userId) {
        LeaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 验证权限
        boolean isTenant = order.getTenantUid().equals(userId);
        Account account = accountMapper.selectById(order.getAccountId());
        boolean isOwner = account != null && account.getOwnerUid().equals(userId);
        
        if (!isTenant && !isOwner) {
            throw new BusinessException(403, "无权限查看此订单");
        }

        OrderVO vo = convertToVO(order);

        // 查询支付信息
        LambdaQueryWrapper<PaymentRecord> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentRecord::getOrderId, orderId)
                .orderByDesc(PaymentRecord::getCreatedAt)
                .last("LIMIT 1");
        PaymentRecord payment = paymentRecordMapper.selectOne(paymentWrapper);
        if (payment != null) {
            vo.setPaymentType(payment.getPaymentType());
            vo.setPaymentStatus(payment.getStatus());
            vo.setPaymentTime(payment.getPaidAt());
            vo.setTransactionId(payment.getTransactionId());
        }

        // 如果是租客且订单状态为租赁中或已完成，返回账号密码（解密）
        if (isTenant && ("leasing".equals(order.getStatus()) || "closed".equals(order.getStatus()))) {
            if (account != null) {
                try {
                    // 使用 AES-256-GCM 解密
                    if (account.getUsernameEnc() != null && !account.getUsernameEnc().isEmpty()) {
                        vo.setUsername(encryptionService.decrypt(account.getUsernameEnc(), account.getId()));
                    }
                    if (account.getPwdEnc() != null && !account.getPwdEnc().isEmpty()) {
                        vo.setPassword(encryptionService.decrypt(account.getPwdEnc(), account.getId()));
                    }
                } catch (Exception e) {
                    log.error("解密账号信息失败: accountId={}, error={}", account.getId(), e.getMessage(), e);
                }
            }
        }

        // 添加账号详细信息
        if (account != null) {
            vo.setAccountLevel(account.getLvl());
            vo.setAccountSkins(account.getSkins());
        }

        // 计算剩余时间（仅租赁中订单）
        if ("leasing".equals(order.getStatus()) && order.getEndTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(order.getEndTime())) {
                long remainingMinutes = java.time.Duration.between(now, order.getEndTime()).toMinutes();
                vo.setRemainingMinutes(remainingMinutes);
            } else {
                vo.setRemainingMinutes(0L);
            }
        }

        return vo;
    }

    @Override
    @Transactional
    public OrderVO renewOrder(Long orderId, Long userId, RenewOrderDTO dto) {
        LeaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 验证权限
        if (!order.getTenantUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 检查订单状态
        if (!"leasing".equals(order.getStatus())) {
            throw new BusinessException(400, "只有租赁中的订单才能续租");
        }

        // 查询账号
        Account account = accountMapper.selectById(order.getAccountId());
        if (account == null) {
            throw new BusinessException(404, "账号不存在");
        }

        // 计算续租时间和价格
        LocalDateTime currentEndTime = order.getEndTime();
        LocalDateTime newEndTime;
        BigDecimal additionalAmount;

        if ("MINUTE".equals(dto.getDurationType())) {
            newEndTime = currentEndTime.plusMinutes(dto.getDuration());
            additionalAmount = account.getPrice30min().multiply(new BigDecimal(dto.getDuration()).divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP));
        } else if ("HOUR".equals(dto.getDurationType())) {
            newEndTime = currentEndTime.plusHours(dto.getDuration());
            additionalAmount = account.getPrice1h().multiply(new BigDecimal(dto.getDuration()));
        } else if ("OVERNIGHT".equals(dto.getDurationType())) {
            newEndTime = currentEndTime.plusDays(1).withHour(8).withMinute(0).withSecond(0);
            additionalAmount = account.getPriceOvernight();
        } else {
            throw new BusinessException(400, "无效的租期类型");
        }

        // 更新订单
        order.setEndTime(newEndTime);
        order.setAmount(order.getAmount().add(additionalAmount));
        orderMapper.updateById(order);

        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO returnAccount(Long orderId, Long userId) {
        LeaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 验证权限
        if (!order.getTenantUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 检查订单状态
        if (!"leasing".equals(order.getStatus())) {
            throw new BusinessException(400, "只有租赁中的订单才能还号");
        }

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
                log.info("订单完成，账号已自动上架: orderId={}, accountId={}", order.getId(), account.getId());
            } else {
                // 有申诉记录，保持账号状态不变，等待申诉处理完成后再处理
                log.info("订单完成，但存在申诉记录，账号状态暂不更新: orderId={}, accountId={}", order.getId(), account.getId());
            }
        }

        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO cancelOrder(Long orderId, Long userId) {
        LeaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 验证权限（只有租客可以取消订单）
        if (!order.getTenantUid().equals(userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 检查订单状态（只有待支付和租赁中的订单可以取消）
        if (!"paying".equals(order.getStatus()) && !"leasing".equals(order.getStatus())) {
            throw new BusinessException(400, "当前订单状态不允许取消");
        }

        // 保存原状态
        String originalStatus = order.getStatus();

        // 更新订单状态
        order.setStatus("cancelled");
        orderMapper.updateById(order);

        // 如果订单是租赁中状态，需要恢复账号状态
        if ("leasing".equals(originalStatus)) {
            Account account = accountMapper.selectById(order.getAccountId());
            if (account != null) {
                account.setStatus(1); // 恢复为上架状态
                accountMapper.updateById(account);
            }
        } else if ("paying".equals(originalStatus)) {
            // 如果订单是待支付状态，也需要恢复账号状态（如果账号已被占用）
            Account account = accountMapper.selectById(order.getAccountId());
            if (account != null && account.getStatus() == 3) {
                account.setStatus(1); // 恢复为上架状态
                accountMapper.updateById(account);
            }
        }

        return convertToVO(order);
    }

    /**
     * 转换为 VO
     */
    private OrderVO convertToVO(LeaseOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 生成订单号
        vo.setOrderNo("ORD" + String.format("%010d", order.getId()));

        // 转换状态为前端状态
        vo.setStatus(OrderStatusConverter.toFrontendStatus(order.getStatus(), order.getEndTime()));

        // 计算总金额（租金 + 押金）
        if (order.getAmount() != null && order.getDeposit() != null) {
            vo.setTotalAmount(order.getAmount().add(order.getDeposit()));
        } else if (order.getAmount() != null) {
            vo.setTotalAmount(order.getAmount());
        }

        // 查询账号信息
        Account account = accountMapper.selectById(order.getAccountId());
        if (account != null) {
            Game game = gameMapper.selectById(account.getGameId());
            vo.setGameName(game != null ? game.getName() : "");
            vo.setOwnerUid(account.getOwnerUid());
            vo.setOwnerId(account.getOwnerUid()); // 设置前端字段名
            vo.setAccountTitle(account.getTitle());
            vo.setAccountDescription(account.getDescription());
            
            // 查询号主信息
            User owner = userMapper.selectById(account.getOwnerUid());
            vo.setOwnerNickname(owner != null ? owner.getNickname() : "");
        }

        // 查询租客信息
        User tenant = userMapper.selectById(order.getTenantUid());
        vo.setTenantNickname(tenant != null ? tenant.getNickname() : "");
        vo.setTenantUid(order.getTenantUid());
        vo.setTenantId(order.getTenantUid()); // 设置前端字段名

        // 计算租期时长（分钟）
        if (order.getStartTime() != null && order.getEndTime() != null) {
            long minutes = java.time.Duration.between(order.getStartTime(), order.getEndTime()).toMinutes();
            vo.setDuration((int) minutes);
        }

        return vo;
    }
}

