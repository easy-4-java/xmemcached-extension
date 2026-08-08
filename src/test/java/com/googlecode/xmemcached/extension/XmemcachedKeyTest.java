package com.googlecode.xmemcached.extension;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link XmemcachedKey} and the related static helpers.
 *
 * @since 3.0.0
 */
class XmemcachedKeyTest {

    @Test
    void shouldExposeDefaultDescriptionForUserGeoLocation() {
        assertEquals("user geo location", XmemcachedKey.USER_GEO_LOCATION.getDesc());
    }

    @Test
    void shouldComposeBaseKeyWhenCalledWithoutArguments() {
        String key = XmemcachedKey.USER_GEO_LOCATION.getKey();
        assertEquals(XmemcachedKey.getKeyStr(XmemcachedKeyConstant.USER_GEO_LOCATION_KEY), key);
        assertTrue(key.endsWith(XmemcachedKeyConstant.USER_GEO_LOCATION_KEY));
    }

    @Test
    void shouldAcceptArbitraryDiscriminatorArgument() {
        String key = XmemcachedKey.USER_GEO_LOCATION.getKey("42");
        assertEquals(XmemcachedKey.getKeyStr(XmemcachedKeyConstant.USER_GEO_LOCATION_KEY), key);
    }

    @Test
    void shouldJoinSegmentsWithDelimiter() {
        String key = XmemcachedKey.getKeyStr("a", "b", "c");
        assertEquals("rds:a:b:c", key);
    }

    @Test
    void shouldPrependRedisPrefixByDefault() {
        String key = XmemcachedKey.getKeyStr("namespace");
        assertTrue(key.startsWith(XmemcachedKey.REDIS_PREFIX + XmemcachedKey.DELIMITER));
        assertEquals("rds:namespace", key);
    }

    @Test
    void shouldSkipNullArguments() {
        String key = XmemcachedKey.getKeyStr("a", null, "b");
        assertEquals("rds:a:b", key);
    }

    @Test
    void shouldSkipEmptyAndWhitespaceArguments() {
        String key = XmemcachedKey.getKeyStr("a", "", "  ", "b");
        assertEquals("rds:a:b", key);
    }

    @Test
    void shouldProduceOnlyPrefixWhenAllArgumentsAreSkipped() {
        String key = XmemcachedKey.getKeyStr(null, "", "   ");
        assertEquals("rds", key);
    }

    @Test
    void shouldExposeStaticDelimiterAndPrefix() {
        assertEquals(":", XmemcachedKey.DELIMITER);
        assertEquals("rds", XmemcachedKey.REDIS_PREFIX);
        assertFalse(XmemcachedKey.DELIMITER.isEmpty());
    }

    @Test
    void shouldIncludeThreadIdInThreadScopedKey() {
        String key = XmemcachedKey.getThreadKeyStr("ns", "part");
        long threadId = Thread.currentThread().getId();
        String expected = "ns" + XmemcachedKey.DELIMITER + threadId + XmemcachedKey.DELIMITER + "part";
        assertEquals(expected, key);
    }

    @Test
    void shouldSkipNullAndEmptySegmentsInThreadScopedKey() {
        long threadId = Thread.currentThread().getId();
        String key = XmemcachedKey.getThreadKeyStr("ns", null, "", "tail");
        assertEquals("ns:" + threadId + ":tail", key);
    }

    @Test
    void shouldProduceThreadKeyEvenWithoutAdditionalArgs() {
        long threadId = Thread.currentThread().getId();
        String key = XmemcachedKey.getThreadKeyStr("ns");
        assertEquals("ns:" + threadId, key);
    }

    @Test
    void shouldHaveOnlyPrivateConstructors() throws NoSuchMethodException {
        Constructor<?>[] constructors = XmemcachedKey.class.getDeclaredConstructors();
        assertTrue(constructors.length >= 1);
        for (Constructor<?> ctor : constructors) {
            assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                    "enum constructor must be private");
        }
    }

    @Test
    void shouldThrowWhenConstructorIsInvokedViaReflection() throws NoSuchMethodException {
        // Enum constructors have synthetic (String name, int ordinal) prefix parameters.
        Constructor<XmemcachedKey> ctor = XmemcachedKey.class.getDeclaredConstructor(
                String.class, int.class, String.class, java.util.function.Function.class);
        ctor.setAccessible(true);
        // The JVM prevents reflective creation of enum constants.
        assertThrows(IllegalArgumentException.class, () ->
                ctor.newInstance("EXTRA", 99, "desc",
                        (java.util.function.Function<Object, String>) obj -> "x"));
    }

    @Test
    void shouldIterateEnumConstants() {
        int count = 0;
        for (XmemcachedKey ignored : XmemcachedKey.values()) {
            count++;
        }
        assertEquals(1, count);
        assertNotNull(XmemcachedKey.valueOf("USER_GEO_LOCATION"));
    }

    @Test
    void shouldRunMainWithoutCrashing() {
        // Main is a developer sanity check; invoking it must never throw.
        XmemcachedKey.main(new String[] {});
    }
}
