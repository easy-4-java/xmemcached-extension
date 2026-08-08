package com.googlecode.xmemcached.extension;

/**
 * Compile-time cache-key constants shared by {@link XmemcachedKey} and the
 * surrounding application code.
 *
 * <p>Centralising these constants prevents typo-driven key collisions and
 * makes it trivial to rename a cache namespace project-wide using a simple
 * IDE refactoring. They are intentionally {@code public static final} so
 * they can be referenced directly without instantiation; the parent class
 * is {@code abstract} to prevent accidental subclassing.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see XmemcachedKey
 */
public abstract class XmemcachedKeyConstant {

	/**
	 * Cache-key fragment used for the user geographic-location bucket
	 * (e.g. last-known latitude/longitude, geofence membership).
	 *
	 * <p>Combined with the {@link XmemcachedKey#REDIS_PREFIX} by
	 * {@link XmemcachedKey#getKeyStr(Object...)} when assembling a full
	 * namespaced key.</p>
	 */
	public final static String USER_GEO_LOCATION_KEY = "user:geo:location";

}
