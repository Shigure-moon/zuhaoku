package com.zhk.infrastructure.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 *
 * @author shigure
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis设置值失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 设置键值对，带过期时间
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Redis设置值失败: key={}, timeout={}, error={}", key, timeout, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis获取值失败: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取值（指定类型）
     *
     * @param key   键
     * @param clazz 类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.error("Redis值类型转换失败: key={}, expectedType={}, error={}", key, clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 删除键
     *
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis删除键失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis检查键是否存在失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key      键
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            return redisTemplate.expire(key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Redis设置过期时间失败: key={}, timeout={}, error={}", key, timeout, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间（秒），-1表示永不过期，-2表示键不存在
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("Redis获取过期时间失败: key={}, error={}", key, e.getMessage(), e);
            return -2L;
        }
    }

    /**
     * 递增
     *
     * @param key 键
     * @return 递增后的值
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis递增失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 递增指定值
     *
     * @param key   键
     * @param delta 增量
     * @return 递增后的值
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis递增失败: key={}, delta={}, error={}", key, delta, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 递减
     *
     * @param key 键
     * @return 递减后的值
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Redis递减失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 根据模式匹配键
     *
     * @param pattern 模式
     * @return 匹配的键集合
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis匹配键失败: pattern={}, error={}", pattern, e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * 获取RedisTemplate（用于高级操作）
     *
     * @return RedisTemplate
     */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}


