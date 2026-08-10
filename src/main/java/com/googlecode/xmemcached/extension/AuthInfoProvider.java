package com.googlecode.xmemcached.extension;

import net.rubyeye.xmemcached.auth.AuthInfo;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy abstraction for supplying per-server {@link AuthInfo} to an
 * xmemcached client.
 *
 * <p>Implementations are typically wired into the bootstrap path of an
 * {@code XMemcachedClient} so that connections to authenticated memcached
 * servers can be opened lazily, on demand, without hard-coding credentials
 * in a static configuration block. The default implementation returns an
 * empty {@link HashMap}, which results in no authentication being attempted.</p>
 *
 * <p>This interface is intentionally Spring-free so that it can be reused in
 * any pure-Java environment.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see AuthInfo
 * @see net.rubyeye.xmemcached.XMemcachedClient#setAuthInfoMap(Map)
 */
public interface AuthInfoProvider {

	/**
	 * Resolve the authentication info map used to negotiate credentials
	 * against the configured memcached servers.
	 *
	 * <p>The returned map is keyed by the {@link InetSocketAddress} of each
	 * server and provides the {@link AuthInfo} (typically {@code plain} or
	 * {@code CRAM-MD5}) used when a session is established. The default
	 * implementation returns an empty map, which causes the client to skip
	 * authentication entirely.</p>
	 *
	 * @return a non-{@code null} map of server address to {@link AuthInfo};
	 *         the map may be empty when no authentication is required
	 */
	default Map<InetSocketAddress, AuthInfo> getAuthInfoMap(){
		return new HashMap<>();
	};

}
