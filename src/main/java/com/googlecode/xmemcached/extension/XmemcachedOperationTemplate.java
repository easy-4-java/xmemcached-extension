package com.googlecode.xmemcached.extension;

import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.Counter;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.transcoders.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * High-level, Spring-free operation facade over an
 * {@link XMemcachedClient} that translates the raw client API into the more
 * familiar {@code cache.get / put / evict} vocabulary.
 *
 * <p>The template hides checked exceptions from the underlying xmemcached
 * API by wrapping every failure into {@link XMemcachedOperationException},
 * provides typed {@code getString} / {@code getLong} / {@code getDouble} /
 * {@code getInteger} accessors with default-value fallbacks, and exposes a
 * collection of ready-to-use {@link Function} converters ({@link #TO_STRING},
 * {@link #TO_LONG}, {@link #TO_DOUBLE}, {@link #TO_INTEGER}) for building
 * custom accessors via {@link #getFor(String, Function)}.</p>
 *
 * <p>All blocking methods honour the {@code opTimeout} supplied to the
 * constructor when performing increment / decrement operations, so callers
 * can tune the latency budget in a single place.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see XMemcachedClient
 * @see XMemcachedOperationException
 * @see XmemcachedKey
 */
@Slf4j
public class XmemcachedOperationTemplate {

    /**
     * Converter that yields the {@code toString()} of any supplied object,
     * or {@code null} when the object itself is {@code null}.
     */
    public static final Function<Object, String> TO_STRING = member -> Objects.toString(member, null);

    /**
     * Converter that parses an object into a {@link Double}, accepting
     * already-typed {@link Double} values without precision loss and falling
     * back to {@link BigDecimal#doubleValue()} for textual representations.
     */
    public static final Function<Object, Double> TO_DOUBLE = member -> {
        if(Objects.isNull(member)) {
            return null;
        }
        return member instanceof Double ? (Double) member : new BigDecimal(member.toString()).doubleValue();
    };

    /**
     * Converter that parses an object into a {@link Long}, accepting
     * already-typed {@link Long} values without precision loss and falling
     * back to {@link BigDecimal#longValue()} for textual representations.
     */
    public static final Function<Object, Long> TO_LONG = member -> {
        if(Objects.isNull(member)) {
            return null;
        }
        return member instanceof Long ? (Long) member : new BigDecimal(member.toString()).longValue();
    };

    /**
     * Converter that parses an object into an {@link Integer}, accepting
     * already-typed {@link Integer} values without precision loss and
     * falling back to {@link BigDecimal#intValue()} for textual
     * representations.
     */
    public static final Function<Object, Integer> TO_INTEGER = member -> {
        if(Objects.isNull(member)) {
            return null;
        }
        return member instanceof Integer ? (Integer) member : new BigDecimal(member.toString()).intValue();
    };

    /**
     * Underlying memcached client used by every operation exposed by this
     * template. Set at construction time and never re-bound.
     */
    MemcachedClient xMemcachedClient;
    /**
     * Cached timeout (in seconds) applied to {@code incr} / {@code decr}
     * variants that accept a {@code long opTimeout} argument. A value of
     * {@code 0} disables the timeout entirely.
     */
    long optTimeout;

    /**
     * Build a new template wrapping the supplied client.
     *
     * @param xMemcachedClient underlying client used to dispatch operations;
     *                          must not be {@code null}
     * @param opTimeout        default per-operation timeout (forwarded to
     *                          {@code incr} / {@code decr}); may be {@code null}
     *                          in which case {@code 0} (no timeout) is used
     * @throws NullPointerException if {@code xMemcachedClient} is {@code null}
     */
    public XmemcachedOperationTemplate(MemcachedClient xMemcachedClient, Duration opTimeout) {
        this.xMemcachedClient = xMemcachedClient;
        this.optTimeout = opTimeout == null ? 0L : opTimeout.getSeconds();
    }

    /**
     * Get or initialise a {@link Counter} bound to the supplied key without
     * supplying an initial value.
     *
     * @param key the counter key; must not be {@code null}
     * @return the {@link Counter} handle, never {@code null}
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Counter counter(String key) {
        try {
            return xMemcachedClient.getCounter(key);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Get or initialise a {@link Counter} bound to the supplied key, seeding
     * it with {@code initialValue} if the key is absent.
     *
     * @param key          the counter key; must not be {@code null}
     * @param initialValue value to use when the key is not yet present
     * @return the {@link Counter} handle, never {@code null}
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Counter counter(String key, long initialValue) {
        try {
            return xMemcachedClient.getCounter(key, initialValue);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Append {@code value} to an existing entry and return the protocol-level
     * success flag.
     *
     * @param key   the key whose value should be appended to
     * @param value the value to append; encoded via the client's default
     *              transcoder
     * @return {@code true} when the server reports a successful append
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public boolean append(String key, Object value) {
        try {
            return xMemcachedClient.append(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Fire-and-forget variant of {@link #append(String, Object)}; the request
     * is queued and acknowledged without waiting for a reply.
     *
     * @param key   the key whose value should be appended to
     * @param value the value to append
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public void appendWithNoReply(String key, Object value) {
        try {
            xMemcachedClient.appendWithNoReply(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Prepend {@code value} to an existing entry and return the protocol-level
     * success flag.
     *
     * @param key   the key whose value should be prepended to
     * @param value the value to prepend; encoded via the client's default
     *              transcoder
     * @return {@code true} when the server reports a successful prepend
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public boolean prepend(String key, Object value) {
        try {
            return xMemcachedClient.prepend(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Fire-and-forget variant of {@link #prepend(String, Object)}; the request
     * is queued and acknowledged without waiting for a reply.
     *
     * @param key   the key whose value should be prepended to
     * @param value the value to prepend
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public void prependWithNoReply(String key, Object value) {
        try {
            xMemcachedClient.prependWithNoReply(key, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Set the value only when the key is currently absent; equivalent to the
     * memcached {@code add} operation. The entry is created with no expiry.
     *
     * @param <T>   value type
     * @param key   the key to insert
     * @param value the value to associate with the key
     * @return {@code true} if the key was inserted; {@code false} if it
     *         already existed
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> boolean setIfAbsent(String key, T value) {
        try {
            return xMemcachedClient.add(key, 0, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Set the value only when the key is currently absent, with an explicit
     * time-to-live.
     *
     * @param <T>     value type
     * @param key     the key to insert
     * @param value   the value to associate with the key
     * @param seconds positive time-to-live in seconds; {@code 0} stores the
     *                entry with no expiry
     * @return {@code true} if the key was inserted; {@code false} if it
     *         already existed
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> boolean setIfAbsent(String key, T value, int seconds) {
        try {
            return xMemcachedClient.add(key, seconds, value);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Compare-and-set wrapper using the memcached {@code gets} /
     * {@code cas} protocol; if the key is absent the value is inserted via
     * {@code add} with no expiry.
     *
     * @param <T>   value type
     * @param key   the key to update
     * @param value the new value to associate with the key
     * @return {@code true} when the server confirms the CAS write or the
     *         initial {@code add} succeeded
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> boolean cas(String key, T value) {
        return this.cas(key, value, 0);
    }

    /**
     * Compare-and-set wrapper using the memcached {@code gets} /
     * {@code cas} protocol with an explicit expiry, falling back to
     * {@code add} when the key is absent.
     *
     * @param <T>     value type
     * @param key     the key to update
     * @param value   the new value to associate with the key
     * @param seconds time-to-live in seconds; {@code 0} for no expiry
     * @return {@code true} when the server confirms the CAS write or the
     *         initial {@code add} succeeded
     * @throws XMemcachedOperationException if the underlying client throws
     */
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

    /**
     * Compare-and-set wrapper using a {@link Duration} for the expiry. A
     * {@code null} or negative duration short-circuits to {@code false}.
     *
     * @param <T>     value type
     * @param key     the key to update
     * @param value   the new value to associate with the key
     * @param timeout positive duration to live; {@code null} or negative
     *                values abort the call
     * @return {@code true} when the server confirms the CAS write
     */
    public <T> boolean cas(String key, T value, Duration timeout) {
        if (Objects.isNull(timeout) || timeout.isNegative()) {
            return false;
        }
        return this.cas(key, value, Long.valueOf(timeout.getSeconds()).intValue());
    }

    /**
     * Compare-and-set wrapper that accepts a pre-computed CAS token. The
     * call is the raw {@code cas(key, seconds, value, cas)} invocation; the
     * caller is responsible for having fetched the token via {@code gets}.
     *
     * @param <T>     value type
     * @param key     the key to update
     * @param value   the new value to associate with the key
     * @param seconds time-to-live in seconds; {@code 0} for no expiry
     * @param cas     CAS token previously obtained from {@code gets}
     * @return {@code true} when the server confirms the CAS write
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> boolean cas(String key, T value, int seconds, long cas) {
        try {
            return xMemcachedClient.cas(key, seconds, value, cas);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Fire-and-forget variant of {@link XMemcachedClient#casWithNoReply}.
     *
     * @param <T>       value type
     * @param key       the key to mutate
     * @param operation application-supplied mutation logic operating on the
     *                  current value
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> void casWithNoReply(String key, CASOperation<T> operation) {
        try {
            xMemcachedClient.casWithNoReply(key, operation);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Store {@code value} under {@code key} with no expiry, overwriting any
     * previous entry.
     *
     * @param <T>   value type
     * @param key   the key to write
     * @param value the value to associate with the key
     * @return {@code true} when the server confirms the write
     * @throws XMemcachedOperationException if the underlying client throws
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
     * Store {@code value} under {@code key} with an explicit time-to-live;
     * non-positive expirations fall back to the no-expiry variant.
     *
     * @param <T>     value type
     * @param key     the key to write
     * @param value   the value to associate with the key
     * @param seconds positive time-to-live in seconds; non-positive values
     *                are coerced to {@code 0}
     * @return {@code true} when the server confirms the write
     * @throws XMemcachedOperationException if the underlying client throws
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
     * Store {@code value} under {@code key} using a {@link Duration} for the
     * time-to-live; a {@code null} or negative duration short-circuits to
     * {@code false}.
     *
     * @param <T>     value type
     * @param key     the key to write
     * @param value   the value to associate with the key
     * @param timeout positive duration to live; {@code null} or negative
     *                values abort the call
     * @return {@code true} when the server confirms the write
     */
    public <T> boolean set(String key, T value, Duration timeout) {
        if (Objects.isNull(timeout) || timeout.isNegative()) {
            return false;
        }
        return set(key, value, Long.valueOf(timeout.getSeconds()).intValue());
    }

    /**
     * Fetch the raw value stored under {@code key} via the client's default
     * transcoder.
     *
     * @param <T> value type expected by the caller
     * @param key the key to read
     * @return the decoded value or {@code null} if the key is missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> T get(String key) {
        try {
            return xMemcachedClient.get(key);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Read the value at {@code key} and coerce it to a {@link String}.
     *
     * @param key the key to read
     * @return the string representation of the stored value, or {@code null}
     *         if the key is missing or the value cannot be stringified
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public String getString(String key) {
        return getFor(key, TO_STRING);
    }

    /**
     * Read the value at {@code key} as a {@link String} with a fallback.
     *
     * @param key        the key to read
     * @param defaultVal value returned when the cache lookup yields
     *                   {@code null}
     * @return the string representation of the stored value, or
     *         {@code defaultVal} when the key is missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public String getString(String key, String defaultVal) {
        String rtVal = getString(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    /**
     * Read the value at {@code key} and coerce it to a {@link Double}.
     *
     * @param key the key to read
     * @return the parsed double, or {@code null} when the key is missing or
     *         cannot be parsed
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Double getDouble(String key) {
        return getFor(key, TO_DOUBLE);
    }

    /**
     * Read the value at {@code key} as a {@link Double} with a fallback.
     *
     * @param key        the key to read
     * @param defaultVal value returned when the cache lookup yields
     *                   {@code null}
     * @return the parsed double, or {@code defaultVal} when missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Double getDouble(String key, double defaultVal) {
        Double rtVal = getDouble(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    /**
     * Read the value at {@code key} and coerce it to a {@link Long}.
     *
     * @param key the key to read
     * @return the parsed long, or {@code null} when the key is missing or
     *         cannot be parsed
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Long getLong(String key) {
        return getFor(key, TO_LONG);
    }

    /**
     * Read the value at {@code key} as a {@link Long} with a fallback.
     *
     * @param key        the key to read
     * @param defaultVal value returned when the cache lookup yields
     *                   {@code null}
     * @return the parsed long, or {@code defaultVal} when missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Long getLong(String key, long defaultVal) {
        Long rtVal = getLong(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    /**
     * Read the value at {@code key} and coerce it to an {@link Integer}.
     *
     * @param key the key to read
     * @return the parsed integer, or {@code null} when the key is missing or
     *         cannot be parsed
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Integer getInteger(String key) {
        return getFor(key, TO_INTEGER);
    }

    /**
     * Read the value at {@code key} as an {@link Integer} with a fallback.
     *
     * @param key        the key to read
     * @param defaultVal value returned when the cache lookup yields
     *                   {@code null}
     * @return the parsed integer, or {@code defaultVal} when missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Integer getInteger(String key, int defaultVal) {
        Integer rtVal = getInteger(key);
        return Objects.nonNull(rtVal) ? rtVal : defaultVal;
    }

    /**
     * Read the value at {@code key} and cast it to the supplied type.
     *
     * @param <T>   target type
     * @param key   the key to read
     * @param clazz the desired target class
     * @return the cast value, or {@code null} when the key is missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> T getFor(String key, Class<T> clazz) {
        return getFor(key, member -> clazz.cast(member));
    }

    /**
     * Read the value at {@code key} and transform it through the supplied
     * mapping function.
     *
     * @param <T>    target type
     * @param key    the key to read
     * @param mapper function applied to the decoded value; must not be
     *               {@code null}
     * @return the mapped value, or {@code null} when the key is missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> T getFor(String key, Function<Object, T> mapper) {
        Object obj = this.get(key);
        if (Objects.nonNull(obj)) {
            return mapper.apply(obj);
        }
        return null;
    }

    /**
     * Read the value at {@code key} using a custom {@link Transcoder}.
     *
     * @param <T>        target type
     * @param key        the key to read
     * @param transcoder transcoder used to decode the value
     * @return the decoded value, or {@code null} if the key is missing
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> T getFor(String key, Transcoder<T> transcoder) {
        try {
            return xMemcachedClient.get(key, transcoder);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new XMemcachedOperationException(e.getMessage());
        }
    }

    /**
     * Batch-read the supplied keys and coerce each value to a {@link Long}.
     *
     * @param keys the keys to read; {@code null} or empty produces an empty
     *             map
     * @return map of key to parsed long; missing keys are absent from the
     *         result map
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Map<String, Long> mGetLong(Collection<String> keys) {
        return mGetFor(keys, TO_LONG);
    }

    /**
     * Batch-read the supplied keys and coerce each value to an
     * {@link Integer}.
     *
     * @param keys the keys to read; {@code null} or empty produces an empty
     *             map
     * @return map of key to parsed integer
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Map<String, Integer> mGetInteger(Collection<String> keys) {
        return mGetFor(keys, TO_INTEGER);
    }

    /**
     * Batch-read the supplied keys and coerce each value to a {@link String}.
     *
     * @param keys the keys to read; {@code null} or empty produces an empty
     *             map
     * @return map of key to string value
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public Map<String, String> mGetString(Collection<String> keys) {
        return mGetFor(keys, TO_STRING);
    }

    /**
     * Batch-read the supplied keys and cast each value to {@code clazz}.
     *
     * @param <T>   target type
     * @param keys  the keys to read; {@code null} or empty produces an empty
     *              map
     * @param clazz the desired target class
     * @return map of key to cast value
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> Map<String, T> mGetFor(Collection<String> keys, Class<T> clazz) {
        return mGetFor(keys, member -> clazz.cast(member));
    }

    /**
     * Batch-read the supplied keys and transform each value through the
     * supplied mapping function.
     *
     * @param <T>    target type
     * @param keys   the keys to read; {@code null} or empty produces an empty
     *               map
     * @param mapper function applied to every decoded value
     * @return map of key to mapped value; missing keys are absent from the
     *         result map
     * @throws XMemcachedOperationException if the underlying client throws
     */
    public <T> Map<String, T> mGetFor(Collection<String> keys, Function<Object, T> mapper) {
        Map<String, Object> members = this.mGet(keys);
        if (Objects.nonNull(members)) {
            return members.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> mapper.apply(e.getValue())));
        }
        return null;
    }

    /**
     * Batch-read the supplied keys using a custom {@link Transcoder}.
     *
     * @param <T>        target type
     * @param keys       the keys to read; {@code null} or empty produces an
     *                   empty map
     * @param transcoder transcoder used to decode every value
     * @return map of key to decoded value
     * @throws XMemcachedOperationException if the underlying client throws
     */
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
     * Batch-read the supplied keys with the client's default transcoder.
     *
     * @param <T>  target type
     * @param keys the keys to read; {@code null} or empty produces an empty
     *             map
     * @return map of key to decoded value
     * @throws XMemcachedOperationException if the underlying client throws
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

    /**
     * Batch-read keys after namespacing each one through
     * {@link XmemcachedKey#getKeyStr(Object...)} with the supplied
     * {@code redisPrefix}.
     *
     * @param <T>         target type
     * @param keys        the raw keys; their {@code toString()} is used as the
     *                    discriminator for namespacing
     * @param redisPrefix first segment of the composite key
     * @return map of fully-qualified key to decoded value
     * @throws XMemcachedOperationException if the underlying client throws
     */
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
     * Atomically add {@code delta} to the counter stored under {@code key}.
     *
     * @param key   the counter key
     * @param delta non-negative increment
     * @return the new counter value after increment
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
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

    /**
     * Fire-and-forget variant of {@link #incr(String, long)}.
     *
     * @param key   the counter key
     * @param delta non-negative increment
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
     */
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
     * Atomically add {@code delta} to the counter stored under {@code key}
     * with an explicit time-to-live; non-positive expirations skip the TTL
     * overload.
     *
     * @param key     the counter key
     * @param delta   non-negative increment
     * @param seconds positive time-to-live in seconds; non-positive values
     *                skip the TTL overload
     * @return the new counter value after increment
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
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

    /**
     * Atomically add {@code delta} to the counter stored under {@code key}
     * using a {@link Duration} for the time-to-live; a {@code null} duration
     * or negative {@link Duration#isNegative()} short-circuits to the
     * no-TTL overload.
     *
     * @param key     the counter key
     * @param delta   non-negative increment
     * @param timeout optional duration; {@code null} or negative values
     *                skip the TTL overload
     * @return the new counter value after increment
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
     */
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
     * Atomically subtract {@code delta} from the counter stored under
     * {@code key}.
     *
     * @param key   the counter key
     * @param delta non-negative decrement
     * @return the new counter value after decrement
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
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

    /**
     * Fire-and-forget variant of {@link #decr(String, long)}.
     *
     * @param key   the counter key
     * @param delta non-negative decrement
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
     */
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
     * Atomically subtract {@code delta} from the counter stored under
     * {@code key} with an explicit time-to-live.
     *
     * @param key     the counter key
     * @param delta   non-negative decrement
     * @param seconds positive time-to-live in seconds; non-positive values
     *                skip the TTL overload
     * @return the new counter value after decrement
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
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

    /**
     * Atomically subtract {@code delta} from the counter stored under
     * {@code key} using a {@link Duration} for the time-to-live; a
     * {@code null} duration or negative {@link Duration#isNegative()}
     * short-circuits to the no-TTL overload.
     *
     * @param key     the counter key
     * @param delta   non-negative decrement
     * @param timeout optional duration; {@code null} or negative values
     *                skip the TTL overload
     * @return the new counter value after decrement
     * @throws XMemcachedOperationException if the underlying client throws or
     *                                       {@code delta} is negative
     */
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
     * Delete one or more keys. {@code null} is tolerated as a no-op.
     *
     * @param keys variable list of keys to delete; {@code null} or an empty
     *             list results in no work being performed
     * @throws XMemcachedOperationException if the underlying client throws
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
