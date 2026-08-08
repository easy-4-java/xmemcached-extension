package com.googlecode.xmemcached.extension;

/**
 * Unchecked exception thrown by {@link XmemcachedOperationTemplate} when an
 * underlying xmemcached call fails.
 *
 * <p>Wraps any {@link Exception} (typically {@code MemcachedException},
 * {@code TimeoutException} or {@code InterruptedException}) raised by
 * {@link net.rubyeye.xmemcached.XMemcachedClient} so that callers of the
 * template only need to handle a single runtime exception type. The original
 * cause is preserved when available.</p>
 *
 * <p>Instances are produced exclusively by {@link XmemcachedOperationTemplate};
 * callers normally do not throw this exception directly.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see XmemcachedOperationTemplate
 */
@SuppressWarnings("serial")
public class XMemcachedOperationException extends RuntimeException {

	/**
	 * Construct a new exception with the supplied detail message and root cause.
	 *
	 * @param msg   human-readable description of the failure (may be {@code null})
	 * @param cause the underlying cause (typically a memcached-protocol error);
	 *              may be {@code null} when no cause is available
	 */
	public XMemcachedOperationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Construct a new exception with the supplied detail message and no root cause.
	 *
	 * @param msg human-readable description of the failure (may be {@code null})
	 */
	public XMemcachedOperationException(String msg) {
		super(msg);
	}

}
