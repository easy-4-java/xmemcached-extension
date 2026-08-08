package com.googlecode.xmemcached.extension;

import net.rubyeye.xmemcached.auth.AuthInfo;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AuthInfoProvider}.
 *
 * <p>Exercises the default implementation and ensures a custom override is
 * honoured as well.</p>
 *
 * @since 3.0.0
 */
class AuthInfoProviderTest {

    @Test
    void shouldReturnEmptyMapByDefault() {
        AuthInfoProvider provider = new AuthInfoProvider() { };
        Map<InetSocketAddress, AuthInfo> map = provider.getAuthInfoMap();

        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    void shouldHonorCustomOverride() {
        Map<InetSocketAddress, AuthInfo> custom = new HashMap<>();
        custom.put(new InetSocketAddress("memcached.local", 11211),
                AuthInfo.plain("user", "pass"));

        AuthInfoProvider provider = new AuthInfoProvider() {
            @Override
            public Map<InetSocketAddress, AuthInfo> getAuthInfoMap() {
                return custom;
            }
        };
        Map<InetSocketAddress, AuthInfo> map = provider.getAuthInfoMap();

        assertEquals(1, map.size());
        AuthInfo info = map.values().iterator().next();
        assertNotNull(info);
        assertNotNull(info.getCallbackHandler());
    }
}
