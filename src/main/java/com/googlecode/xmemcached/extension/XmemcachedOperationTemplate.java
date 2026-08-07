package com.googlecode.xmemcached.extension;

import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.Counter;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.transcoders.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class XmemcachedOperationTemplate {

    public static final Function<Object, String> TO_STRING = member -> Objects.toString(member, null);

    public static final Function<Object, Double> TO_DOUBLE = member -> {
        if(Objects.isNull(member)) {
            return null;
        }
        return member instanceof Double ? (Double) member : new BigDecimal(member.toString()).doubleValue();
    };

    public static final Function<Object, Long> TO_LONG = member -> {
        if(Objects.isNull(member)) {
            return null;
        }
        return member instanceof Long ? (Long) member : new BigDecimal(member.toString()).longValue();
    };

    public static final Function<Object, Integer> TO_INTEGER = member -> {
        if(Objects.isNull(member)) {
            return null;
        }
        return member instanceof Integer ? (Integer) member : new BigDecimal(member.toString()).intValue();
    };

    XMemcachedClient xMemcachedClient;
    long optTimeout;

    public XmemcachedOperationTemplate(XMemcachedClient xMemcachedClient, Duration opTimeout) {
        this.xMemcachedClient = xMemcachedClient;
        this.optTimeout = opTimeout == null ? 0L : opTimeout.getSeconds();
    }

    public Counter counter(String key) {
        try {
            return xMemcachedClient.getCounter(key);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public Counter counter(String key, long initialValue) {
        try {
            return xMemcachedClient.getCounter(key, initialValue);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public boolean append(String key, Object value) {
        try {
            return xMemcachedClient.append(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public void appendWithNoReply(String key, Object value) {
        try {
            xMemcachedClient.appendWithNoReply(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public boolean prepend(String key, Object value) {
        try {
            return xMemcachedClient.prepend(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public void prependWithNoReply(String key, Object value) {
        try {
            xMemcachedClient.prependWithNoReply(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public <T> boolean setIfAbsent(String key, T value) {
        try {
            return xMemcachedClient.add(key, 0, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public <T> boolean setIfAbsent(String key, T value, int seconds) {
        try {
            return xMemcachedClient.add(key, seconds, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public <T> boolean cas(String key, T value) {
        return this.cas(key, value, 0);
    }

    public <T> boolean cas(String key, T value, int seconds) {
        try {
            GetsResponse<Object> result = xMemcachedClient.gets(key);
            if(Objects.isNull(result)){
                return xMemcachedClient.add(key, seconds, value);
            }
            return xMemcachedClient.cas(key, seconds, value, result.getCas());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public <T> boolean cas(String key, T value, Duration timeout) {
        if (Objects.isNull(timeout) || timeout.isNegative()) {
            return false;
        }
        return this.cas(key, value, Long.valueOf(timeout.getSeconds()).intValue());
    }

    public <T> boolean cas(String key, T value, int seconds, long cas) {
        try {
            return xMemcachedClient.cas(key, seconds, value, cas);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public <T> void casWithNoReply(String key, CASOperation<T> operation) {
        try {
            xMemcachedClient.casWithNoReply(key, operation);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public <T> boolean set(String key, T value) {
        try {
            return xMemcachedClient.set(key, 0, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key     缓存key
     * @param value   缓存值
     * @param seconds 过期时间(秒) time要&gt;=0 如果time等于0，表示永久存储（默认是一个月)
     * @return true成功 false 失败
     */
    public <T> boolean set(String key, T value, int seconds) {
        try {
            if (seconds > 0) {
                return xMemcachedClient.set(key, seconds, value);
            } else {
                return set(key, value);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key     缓存key
     * @param value   缓存值
     * @param timeout 时间
     * @return true成功 false 失败
     */
    public <T> boolean set(String key, T value, Duration timeout) {
        if (Objects.isNull(timeout) || timeout.isNegative()) {
            return false;
        }
        return set(key, value, Long.valueOf(timeout.getSeconds()).intValue());
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public <T> T get(String key) {
        try {
            return xMemcachedClient.get(key);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public String getString(String key) {
        return getFor(key, TO_STRING);
    }

    public String getString(String key, String defaultVal) {
        String rtVal = getString(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    public Double getDouble(String key) {
        return getFor(key, TO_DOUBLE);
    }

    public Double getDouble(String key, double defaultVal) {
        Double rtVal = getDouble(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    public Long getLong(String key) {
        return getFor(key, TO_LONG);
    }

    public Long getLong(String key, long defaultVal) {
        Long rtVal = getLong(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    public Integer getInteger(String key) {
        return getFor(key, TO_INTEGER);
    }

    public Integer getInteger(String key, int defaultVal) {
        Integer rtVal = getInteger(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    public <T> T getFor(String key, Class<T> clazz) {
        return getFor(key, member -> clazz.cast(member));
    }

    public <T> T getFor(String key, Function<Object, T> mapper) {
        Object obj = this.get(key);
        if (Objects.nonNull(obj)) {
            return mapper.apply(obj);
        }
        return null;
    }

    public <T> T getFor(String key, Transcoder<T> transcoder) {
        try {
            return xMemcachedClient.get(key, transcoder);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public Map<String, Long> mGetLong(Collection<String> keys) {
        return mGetFor(keys, TO_LONG);
    }

    public Map<String, Integer> mGetInteger(Collection<String> keys) {
        return mGetFor(keys, TO_INTEGER);
    }

    public Map<String, String> mGetString(Collection<String> keys) {
        return mGetFor(keys, TO_STRING);
    }

    public <T> Map<String, T> mGetFor(Collection<String> keys, Class<T> clazz) {
        return mGetFor(keys, member -> clazz.cast(member));
    }

    public <T> Map<String, T> mGetFor(Collection<String> keys, Function<Object, T> mapper) {
        Map<String, Object> members = this.mGet(keys);
        if (Objects.nonNull(members)) {
            return members.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, mapper));
        }
        return null;
    }

    public <T> Map<String, T> mGetFor(Collection<String> keys, Transcoder<T> transcoder) {
        try {
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyMap();
            }
            return xMemcachedClient.get(keys, transcoder);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 批量获取缓存值
     *
     * @param keys 键集合
     * @return 值
     */
    public <T> Map<String, T> mGet(Collection<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyMap();
            }
            return xMemcachedClient.get(keys);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public <T> Map<String, T> mGet(Collection<Object> keys, String redisPrefix) {
        try {
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyMap();
            }
            Collection<String> newKeys = keys.stream().map(key -> XmemcachedKey.getKeyStr(redisPrefix, key.toString())).collect(Collectors.toList());
            return xMemcachedClient.get(newKeys);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(&gt;=0)
     * @return 增加指定数值后的值
     */
    public Long incr(String key, long delta) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递增因子必须>=0");
        }
        try {
            return xMemcachedClient.incr(key, delta);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public void incrWithNoReply(String key, long delta) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递增因子必须>=0");
        }
        try {
            xMemcachedClient.incrWithNoReply(key, delta);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 递增
     *
     * @param key     键
     * @param delta   要增加几(&gt;=0)
     * @param seconds 过期时长（秒）
     * @return 增加指定数值后的值
     */
    public Long incr(String key, long delta, int seconds) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递增因子必须>=0");
        }
        try {
            if (seconds > 0) {
                return xMemcachedClient.incr(key, delta, 0, optTimeout, seconds);
            }
            return xMemcachedClient.incr(key, delta, 0);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public Long incr(String key, long delta, Duration timeout) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递增因子必须>=0");
        }
        try {
            if (!timeout.isNegative()) {
                return xMemcachedClient.incr(key, delta, 0, optTimeout, Long.valueOf(timeout.getSeconds()).intValue());
            }
            return xMemcachedClient.incr(key, delta, 0);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(&gt;=0)
     * @return 减少指定数值后的值
     */
    public Long decr(String key, long delta) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递减因子必须>=0");
        }
        try {
            return xMemcachedClient.decr(key, delta);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public void decrWithNoReply(String key, long delta) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递减因子必须>=0");
        }
        try {
            xMemcachedClient.decrWithNoReply(key, delta);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 递减
     *
     * @param key     键
     * @param delta   要减少几(&gt;=0)
     * @param seconds 过期时长（秒）
     * @return 减少指定数值后的值
     */
    public Long decr(String key, long delta, int seconds) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递减因子必须>=0");
        }
        try {
            if (seconds > 0) {
                return xMemcachedClient.decr(key, delta, 0, optTimeout, seconds);
            }
            return xMemcachedClient.decr(key, delta, 0);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    public Long decr(String key, long delta, Duration timeout) {
        if (delta < 0) {
            throw new XMemcachedOperationException("递减因子必须>=0");
        }
        try {
            if (!timeout.isNegative()) {
                return xMemcachedClient.decr(key, delta, 0, optTimeout, Long.valueOf(timeout.getSeconds()).intValue());
            }
            return xMemcachedClient.decr(key, delta, 0);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * 删除缓存
     * @param keys 可以传一个值 或多个
     */
    public void del(String... keys) {
        try {
            if (Objects.nonNull(keys)) {
                for (String key : keys) {
                    xMemcachedClient.delete(key);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

}