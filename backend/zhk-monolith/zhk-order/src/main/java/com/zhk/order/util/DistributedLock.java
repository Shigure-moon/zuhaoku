package com.zhk.order.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工具类
 * 基于 Redis 实现简单的分布式锁
 * 如果 Redis 不可用，则降级为本地锁（不保证分布式一致性）
 *
 * @author shigure
 */
@Slf4j
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_TIMEOUT = 10; // 默认锁超时时间（秒）
    private static final long DEFAULT_WAIT_TIME = 3; // 默认等待时间（秒）

    /**
     * 尝试获取锁
     *
     * @param key 锁的键
     * @return 是否获取成功
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取锁（带超时时间）
     *
     * @param key      锁的键
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String key, long timeout, TimeUnit timeUnit) {
        String lockKey = LOCK_PREFIX + key;
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", timeout, timeUnit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 尝试获取锁（带等待时间）
     *
     * @param key      锁的键
     * @param waitTime 等待时间（秒）
     * @param timeout  锁超时时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLockWithWait(String key, long waitTime, long timeout) {
        long endTime = System.currentTimeMillis() + waitTime * 1000;
        
        while (System.currentTimeMillis() < endTime) {
            if (tryLock(key, timeout, TimeUnit.SECONDS)) {
                return true;
            }
            try {
                Thread.sleep(100); // 等待100ms后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 释放锁
     *
     * @param key 锁的键
     */
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("释放分布式锁失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 执行带锁的操作
     *
     * @param key      锁的键
     * @param callback 回调函数
     * @param <T>     返回值类型
     * @return 回调函数的返回值
     * @throws RuntimeException 如果获取锁失败或执行异常
     */
    public <T> T executeWithLock(String key, LockCallback<T> callback) {
        return executeWithLock(key, DEFAULT_WAIT_TIME, DEFAULT_TIMEOUT, callback);
    }

    /**
     * 执行带锁的操作（带参数）
     *
     * @param key      锁的键
     * @param waitTime 等待时间（秒）
     * @param timeout  锁超时时间（秒）
     * @param callback 回调函数
     * @param <T>     返回值类型
     * @return 回调函数的返回值
     * @throws RuntimeException 如果获取锁失败或执行异常
     */
    public <T> T executeWithLock(String key, long waitTime, long timeout, LockCallback<T> callback) {
        if (!tryLockWithWait(key, waitTime, timeout)) {
            throw new RuntimeException("获取锁失败，请稍后重试");
        }
        
        try {
            return callback.execute();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("执行带锁操作时发生异常", e);
        } finally {
            unlock(key);
        }
    }

    /**
     * 锁回调接口
     */
    @FunctionalInterface
    public interface LockCallback<T> {
        T execute() throws Exception;
    }
}

