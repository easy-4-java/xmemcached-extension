package com.googlecode.xmemcached.extension;

import com.googlecode.xmemcached.extension.util.Strings;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Enumerated catalogue of well-known memcached cache namespaces, each bound to
 * a key-building {@link Function}.
 *
 * <p>Each enum constant carries a human-readable {@link #getDesc() description}
 * plus a {@link Function} that turns an arbitrary user-supplied
 * {@link Object} into a fully qualified, delimiter-joined key. Centralising
 * the namespace construction guarantees that two callers asking for the same
 * logical entry will always generate the same physical key.</p>
 *
 * <p>The static helpers {@link #getKeyStr(Object...)} and
 * {@link #getThreadKeyStr(String, Object...)} produce raw key strings without
 * needing an enum constant and are also reused internally by
 * {@link XmemcachedOperationTemplate}.</p>
 *
 * @author wandl
 * @since 3.0.0
 * @see XmemcachedKeyConstant
 * @see XmemcachedOperationTemplate
 */
public enum XmemcachedKey {

	/**
	 * User geographic-location bucket. See
	 * {@link XmemcachedKeyConstant#USER_GEO_LOCATION_KEY}.
	 */
	USER_GEO_LOCATION("user geo location", (userId)->{
		return getKeyStr(XmemcachedKeyConstant.USER_GEO_LOCATION_KEY);
    })

	;

	/**
	 * Human-readable description of this cache namespace.
	 */
	private String desc;
    /**
     * Builder function producing the fully-qualified key for this namespace
     * given an optional user-supplied discriminator.
     */
    private Function<Object, String> function;

    /**
     * Internal enum constructor; intentionally {@code private} per the
     * Java language specification for enums.
     *
     * @param desc     human-readable description shown in logs and tooling
     * @param function key-building function applied via {@link #getKey()} or
     *                 {@link #getKey(Object)}
     */
    XmemcachedKey(String desc, Function<Object, String> function) {
        this.desc = desc;
        this.function = function;
    }

    /**
     * Return the human-readable description of this cache namespace.
     *
     * @return the description supplied at enum-constant declaration time
     */
    public String getDesc() {
		return desc;
	}

    /**
     * Get the fully-qualified memcached key for this namespace, without any
     * extra discriminator argument.
     *
     * @return the key produced by applying {@code null} to the configured
     *         {@link Function}; never {@code null}
     */
    public String getKey() {
        return this.function.apply(null);
    }

    /**
     * Get the fully-qualified memcached key for this namespace, optionally
     * combined with a user-supplied discriminator argument.
     *
     * @param key optional discriminator appended to the namespace; may be
     *            {@code null} or non-text, in which case the underlying
     *            {@link #getKeyStr(Object...)} helper will simply skip it
     * @return the fully-qualified key string; never {@code null}
     */
    public String getKey(Object key) {
        return this.function.apply(key);
    }

    /**
     * Common cache-key prefix applied by {@link #getKeyStr(Object...)} so
     * that the underlying memcached instance can partition traffic across
     * logical applications if desired.
     */
    public static String REDIS_PREFIX = "rds";

    /**
     * Field-level delimiter used to join the segments of a composite
     * cache key (e.g. {@code "rds:user:geo:location:42"}).
     */
    public final static String DELIMITER = ":";

    /**
     * Build a fully-qualified cache key by joining the configured
     * {@link #REDIS_PREFIX} with the supplied segments using
     * {@link #DELIMITER}.
     *
     * <p>{@code null} arguments and empty / whitespace-only {@code toString()}
     * representations are silently skipped, mirroring the behaviour of
     * {@link com.googlecode.xmemcached.extension.util.Strings#hasText(CharSequence)}.</p>
     *
     * @param args variable-length list of key segments; may include
     *             {@code null} or text-less entries which will be ignored
     * @return a non-{@code null} delimiter-joined key string; empty if no
     *         textual segment was supplied
     * @see com.googlecode.xmemcached.extension.util.Strings#hasText(CharSequence)
     */
    public static String getKeyStr(Object... args) {
        StringJoiner tempKey = new StringJoiner(DELIMITER);
        tempKey.add(REDIS_PREFIX);
        for (Object s : args) {
            if (Objects.isNull(s) || !Strings.hasText(s.toString())) {
                continue;
            }
            tempKey.add(s.toString());
        }
        return tempKey.toString();
    }

    /**
     * Build a cache key scoped to the calling thread by joining the supplied
     * {@code prefix}, the current {@link Thread#getId() thread id} and any
     * extra arguments.
     *
     * <p>Useful for caches that should not be shared across worker threads
     * (e.g. request-scoped or batch-scoped counters).</p>
     *
     * @param prefix first segment of the key (typically a logical namespace)
     * @param args   optional additional segments; {@code null} or text-less
     *               entries are silently skipped
     * @return a non-{@code null} delimiter-joined key string; the current
     *         thread id is always included as the second segment
     */
    public static String getThreadKeyStr(String prefix, Object... args) {

        StringJoiner tempKey = new StringJoiner(DELIMITER);
        tempKey.add(prefix);
        tempKey.add(String.valueOf(Thread.currentThread().getId()));
        for (Object s : args) {
            if (Objects.isNull(s) || !Strings.hasText(s.toString())) {
                continue;
            }
            tempKey.add(s.toString());
        }
        return tempKey.toString();
    }

    /**
     * Local entry point used during development to sanity-check the
     * {@link #getKeyStr(Object...)} helper.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.println(getKeyStr(233,""));
    }


}
