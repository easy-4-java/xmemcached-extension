package com.googlecode.xmemcached.extension;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.Counter;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.CompressionMode;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link XmemcachedOperationTemplate}.
 *
 * <p>Uses a JDK dynamic proxy implementing {@link MemcachedClient} to
 * exercise every method on the template without requiring a live memcached
 * server.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see XmemcachedOperationTemplate
 */
class XmemcachedOperationTemplateTest {

    // ------------------------------------------------------------------ helpers

    /**
     * Build a dummy {@link Transcoder} with the given decode function.
     * All other methods are no-ops.
     */
    private static <T> Transcoder<T> dummyTranscoder(java.util.function.Function<CachedData, T> decodeFn) {
        return new Transcoder<>() {
            @Override public T decode(CachedData d) { return decodeFn.apply(d); }
            @Override public CachedData encode(T o) { return null; }
            @Override public void setPrimitiveAsString(boolean b) {}
            @Override public void setPackZeros(boolean b) {}
            @Override public void setCompressionThreshold(int i) {}
            @Override public boolean isPrimitiveAsString() { return false; }
            @Override public boolean isPackZeros() { return false; }
            @Override public void setCompressionMode(CompressionMode mode) {}
        };
    }

    /**
     * Build a proxy that delegates every call to the supplied handler.
     */
    private static MemcachedClient proxyClient(InvocationHandler handler) {
        return (MemcachedClient) Proxy.newProxyInstance(
                MemcachedClient.class.getClassLoader(),
                new Class<?>[]{MemcachedClient.class},
                handler);
    }

    /**
     * Handler that returns sensible defaults for every known method so
     * success-path tests can be written concisely.
     */
    private static MemcachedClient defaultClient() {
        // Use a holder so the handler can reference the proxy itself (needed for Counter).
        final MemcachedClient[] holder = new MemcachedClient[1];
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            String name = method.getName();
            return switch (name) {
                case "get" -> {
                    if (args.length == 1 && args[0] instanceof String) yield "value";
                    if (args.length == 2 && args[0] instanceof String && args[1] instanceof Transcoder) yield "tcValue";
                    if (args.length == 1 && args[0] instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<String> keys = (Collection<String>) args[0];
                        Map<String, Object> map = new HashMap<>();
                        keys.forEach(k -> map.put(k, "v_" + k));
                        yield map;
                    }
                    if (args.length == 2 && args[0] instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<String> keys = (Collection<String>) args[0];
                        Map<String, Object> map = new HashMap<>();
                        keys.forEach(k -> map.put(k, "v_" + k));
                        yield map;
                    }
                    yield null;
                }
                case "gets" -> new GetsResponse<>(42L, "getsValue");
                case "set", "add", "cas", "replace" -> Boolean.TRUE;
                case "append", "prepend" -> Boolean.TRUE;
                case "delete" -> Boolean.TRUE;
                case "incr", "decr" -> {
                    if (args.length >= 3) yield 10L;   // with initialValue/TTL
                    yield 1L;                            // simple
                }
                case "getCounter" -> {
                    if (args.length == 2) yield new Counter(holder[0], (String) args[0], (Long) args[1]);
                    yield new Counter(holder[0], (String) args[0], 0L);
                }
                default -> null;
            };
        });
        holder[0] = client;
        return client;
    }

    /**
     * Handler that throws {@link MemcachedException} on every call.
     */
    private static MemcachedClient failingClient() {
        return proxyClient((proxy, method, args) -> {
            throw new MemcachedException("mock failure for " + method.getName());
        });
    }

    // --------------------------------------------------------------- converters

    @Test
    void shouldConvertToString() {
        assertEquals("42", XmemcachedOperationTemplate.TO_STRING.apply(42));
        assertNull(XmemcachedOperationTemplate.TO_STRING.apply(null));
    }

    @Test
    void shouldConvertToDouble() {
        assertEquals(3.14, XmemcachedOperationTemplate.TO_DOUBLE.apply(3.14));
        assertEquals(3.0, XmemcachedOperationTemplate.TO_DOUBLE.apply("3.0"));
        assertNull(XmemcachedOperationTemplate.TO_DOUBLE.apply(null));
    }

    @Test
    void shouldConvertToLong() {
        assertEquals(100L, XmemcachedOperationTemplate.TO_LONG.apply(100L));
        assertEquals(200L, XmemcachedOperationTemplate.TO_LONG.apply("200"));
        assertNull(XmemcachedOperationTemplate.TO_LONG.apply(null));
    }

    @Test
    void shouldConvertToInteger() {
        assertEquals(7, XmemcachedOperationTemplate.TO_INTEGER.apply(7));
        assertEquals(8, XmemcachedOperationTemplate.TO_INTEGER.apply("8"));
        assertNull(XmemcachedOperationTemplate.TO_INTEGER.apply(null));
    }

    // ----------------------------------------------------------- constructor

    @Test
    void shouldAcceptNullTimeout() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(0L, template.optTimeout);
    }

    @Test
    void shouldStoreTimeoutInSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), Duration.ofSeconds(5));
        assertEquals(5L, template.optTimeout);
    }

    // ----------------------------------------------------------- counter

    @Test
    void shouldReturnCounterWithoutInitialValue() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Counter c = template.counter("cnt");
        assertNotNull(c);
    }

    @Test
    void shouldReturnCounterWithInitialValue() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Counter c = template.counter("cnt", 100L);
        assertNotNull(c);
    }

    @Test
    void shouldWrapExceptionInCounter() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.counter("cnt"));
    }

    @Test
    void shouldWrapExceptionInCounterWithInitialValue() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.counter("cnt", 1L));
    }

    // ----------------------------------------------------------- append

    @Test
    void shouldAppendSuccessfully() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.append("k", "v"));
    }

    @Test
    void shouldWrapExceptionInAppend() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.append("k", "v"));
    }

    @Test
    void shouldAppendWithNoReply() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertDoesNotThrow(() -> template.appendWithNoReply("k", "v"));
    }

    @Test
    void shouldWrapExceptionInAppendWithNoReply() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.appendWithNoReply("k", "v"));
    }

    // ----------------------------------------------------------- prepend

    @Test
    void shouldPrependSuccessfully() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.prepend("k", "v"));
    }

    @Test
    void shouldWrapExceptionInPrepend() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.prepend("k", "v"));
    }

    @Test
    void shouldPrependWithNoReply() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertDoesNotThrow(() -> template.prependWithNoReply("k", "v"));
    }

    @Test
    void shouldWrapExceptionInPrependWithNoReply() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.prependWithNoReply("k", "v"));
    }

    // ----------------------------------------------------------- setIfAbsent

    @Test
    void shouldSetIfAbsentWithoutTtl() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.setIfAbsent("k", "v"));
    }

    @Test
    void shouldSetIfAbsentWithTtl() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.setIfAbsent("k", "v", 60));
    }

    @Test
    void shouldWrapExceptionInSetIfAbsent() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.setIfAbsent("k", "v"));
    }

    @Test
    void shouldWrapExceptionInSetIfAbsentWithTtl() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.setIfAbsent("k", "v", 10));
    }

    // ----------------------------------------------------------- set

    @Test
    void shouldSetWithoutTtl() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.set("k", "v"));
    }

    @Test
    void shouldSetWithPositiveTtl() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.set("k", "v", 60));
    }

    @Test
    void shouldFallbackToNoTtlWhenSecondsNonPositive() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.set("k", "v", 0));
        assertTrue(template.set("k", "v", -1));
    }

    @Test
    void shouldSetWithPositiveDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.set("k", "v", Duration.ofMinutes(2)));
    }

    @Test
    void shouldReturnFalseForNullDurationSet() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertFalse(template.set("k", "v", (Duration) null));
    }

    @Test
    void shouldReturnFalseForNegativeDurationSet() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertFalse(template.set("k", "v", Duration.ofSeconds(-1)));
    }

    @Test
    void shouldWrapExceptionInSet() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.set("k", "v"));
    }

    @Test
    void shouldWrapExceptionInSetWithSeconds() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.set("k", "v", 60));
    }

    // ----------------------------------------------------------- cas

    @Test
    void shouldCasWhenKeyExists() {
        AtomicReference<Boolean> getsCalled = new AtomicReference<>(false);
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("gets".equals(method.getName())) {
                getsCalled.set(true);
                return new GetsResponse<>(99L, "old");
            }
            if ("cas".equals(method.getName())) return Boolean.TRUE;
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        assertTrue(template.cas("k", "new"));
        assertTrue(getsCalled.get());
    }

    @Test
    void shouldCasViaAddWhenKeyAbsent() {
        AtomicReference<Boolean> addCalled = new AtomicReference<>(false);
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("gets".equals(method.getName())) return null;
            if ("add".equals(method.getName())) {
                addCalled.set(true);
                return Boolean.TRUE;
            }
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        assertTrue(template.cas("k", "new"));
        assertTrue(addCalled.get());
    }

    @Test
    void shouldCasWithExplicitSeconds() {
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("gets".equals(method.getName())) return new GetsResponse<>(1L, "v");
            if ("cas".equals(method.getName())) return Boolean.TRUE;
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        assertTrue(template.cas("k", "v", 30));
    }

    @Test
    void shouldCasWithPositiveDuration() {
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("gets".equals(method.getName())) return new GetsResponse<>(1L, "v");
            if ("cas".equals(method.getName())) return Boolean.TRUE;
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        assertTrue(template.cas("k", "v", Duration.ofSeconds(30)));
    }

    @Test
    void shouldReturnFalseForNullDurationCas() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertFalse(template.cas("k", "v", (Duration) null));
    }

    @Test
    void shouldReturnFalseForNegativeDurationCas() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertFalse(template.cas("k", "v", Duration.ofSeconds(-1)));
    }

    @Test
    void shouldCasWithExplicitCasToken() {
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("cas".equals(method.getName())) return Boolean.TRUE;
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        assertTrue(template.cas("k", "v", 30, 12345L));
    }

    @Test
    void shouldWrapExceptionInCas() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.cas("k", "v"));
    }

    @Test
    void shouldWrapExceptionInCasWithSeconds() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.cas("k", "v", 10));
    }

    @Test
    void shouldWrapExceptionInCasWithCasToken() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.cas("k", "v", 10, 1L));
    }

    @Test
    void shouldCasWithNoReply() {
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("casWithNoReply".equals(method.getName())) return null;
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        CASOperation<Object> op = new CASOperation<>() {
            @Override public int getMaxTries() { return 1; }
            @Override public Object getNewValue(long currentCAS, Object currentValue) { return "x"; }
        };
        assertDoesNotThrow(() -> template.casWithNoReply("k", op));
    }

    @Test
    void shouldWrapExceptionInCasWithNoReply() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        CASOperation<Object> op = new CASOperation<>() {
            @Override public int getMaxTries() { return 1; }
            @Override public Object getNewValue(long currentCAS, Object currentValue) { return "x"; }
        };
        assertThrows(XMemcachedOperationException.class, () -> template.casWithNoReply("k", op));
    }

    // ----------------------------------------------------------- get

    @Test
    void shouldGetValue() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals("value", template.get("k"));
    }

    @Test
    void shouldWrapExceptionInGet() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.get("k"));
    }

    // ----------------------------------------------------------- getString

    @Test
    void shouldGetString() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals("value", template.getString("k"));
    }

    @Test
    void shouldGetStringWithDefault() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals("value", template.getString("k", "fallback"));
    }

    @Test
    void shouldReturnDefaultWhenGetReturnsNull() {
        MemcachedClient client = proxyClient((proxy, method, args) -> null);
        var template = new XmemcachedOperationTemplate(client, null);
        assertNull(template.getString("k"));
        assertEquals("fallback", template.getString("k", "fallback"));
    }

    // ----------------------------------------------------------- getDouble

    @Test
    void shouldGetDouble() {
        MemcachedClient client = proxyClient((proxy, method, args) -> 3.14);
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals(3.14, template.getDouble("k"));
    }

    @Test
    void shouldGetDoubleWithDefault() {
        MemcachedClient client = proxyClient((proxy, method, args) -> null);
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals(9.99, template.getDouble("k", 9.99));
    }

    // ----------------------------------------------------------- getLong

    @Test
    void shouldGetLong() {
        MemcachedClient client = proxyClient((proxy, method, args) -> 42L);
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals(42L, template.getLong("k"));
    }

    @Test
    void shouldGetLongWithDefault() {
        MemcachedClient client = proxyClient((proxy, method, args) -> null);
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals(99L, template.getLong("k", 99L));
    }

    // ----------------------------------------------------------- getInteger

    @Test
    void shouldGetInteger() {
        MemcachedClient client = proxyClient((proxy, method, args) -> 7);
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals(7, template.getInteger("k"));
    }

    @Test
    void shouldGetIntegerWithDefault() {
        MemcachedClient client = proxyClient((proxy, method, args) -> null);
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals(55, template.getInteger("k", 55));
    }

    // ----------------------------------------------------------- getFor

    @Test
    void shouldGetForWithClass() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals("value", template.getFor("k", String.class));
    }

    @Test
    void shouldGetForWithFunction() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals("VALUE", template.getFor("k", v -> ((String) v).toUpperCase()));
    }

    @Test
    void shouldReturnNullFromGetForWhenKeyMissing() {
        MemcachedClient client = proxyClient((proxy, method, args) -> null);
        var template = new XmemcachedOperationTemplate(client, null);
        assertNull(template.getFor("k", v -> v));
    }

    @Test
    void shouldGetForWithTranscoder() {
        Transcoder<String> tc = dummyTranscoder(d -> "tcDecoded");
        MemcachedClient client = proxyClient((proxy, method, args) -> {
            if ("get".equals(method.getName()) && args.length == 2) return "tcVal";
            return null;
        });
        var template = new XmemcachedOperationTemplate(client, null);
        assertEquals("tcVal", template.getFor("k", tc));
    }

    @Test
    void shouldWrapExceptionInGetForWithTranscoder() {
        Transcoder<String> tc = dummyTranscoder(d -> null);
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.getFor("k", tc));
    }

    // ----------------------------------------------------------- mGet

    @Test
    void shouldMGet() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Map<String, Object> result = template.mGet(List.of("k1", "k2"));
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyMapForNullKeysMGet() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.mGet(null).isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForEmptyKeysMGet() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.mGet(Collections.emptyList()).isEmpty());
    }

    @Test
    void shouldWrapExceptionInMGet() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.mGet(List.of("k")));
    }

    // ----------------------------------------------------------- mGet with prefix

    @Test
    void shouldMGetWithPrefix() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Map<String, Object> result = template.mGet(List.of("id1", "id2"), "prefix");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForNullKeysMGetWithPrefix() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.mGet(null, "p").isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForEmptyKeysMGetWithPrefix() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.mGet(Collections.emptyList(), "p").isEmpty());
    }

    @Test
    void shouldWrapExceptionInMGetWithPrefix() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.mGet(List.of("k"), "p"));
    }

    // ----------------------------------------------------------- mGetFor

    @Test
    void shouldMGetForWithFunction() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Map<String, String> result = template.mGetString(List.of("k1", "k2"));
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldMGetForWithClass() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Map<String, String> result = template.mGetFor(List.of("k1"), String.class);
        assertNotNull(result);
    }

    @Test
    void shouldReturnNullFromMGetForWhenMGetReturnsNull() {
        MemcachedClient client = proxyClient((proxy, method, args) -> null);
        var template = new XmemcachedOperationTemplate(client, null);
        assertNull(template.mGetFor(List.of("k"), v -> v));
    }

    @Test
    void shouldMGetForWithTranscoder() {
        Transcoder<String> tc = dummyTranscoder(d -> null);
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        Map<String, String> result = template.mGetFor(List.of("k1"), tc);
        assertNotNull(result);
    }

    @Test
    void shouldReturnEmptyMapForNullKeysMGetForWithTranscoder() {
        Transcoder<String> tc = dummyTranscoder(d -> null);
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.mGetFor((Collection<String>) null, tc).isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForEmptyKeysMGetForWithTranscoder() {
        Transcoder<String> tc = dummyTranscoder(d -> null);
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertTrue(template.mGetFor(Collections.emptyList(), tc).isEmpty());
    }

    @Test
    void shouldWrapExceptionInMGetForWithTranscoder() {
        Transcoder<String> tc = dummyTranscoder(d -> null);
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.mGetFor(List.of("k"), tc));
    }

    // ----------------------------------------------------------- mGetLong / mGetInteger / mGetString

    @Test
    void shouldMGetLong() {
        // Use a client that returns numeric values so TO_LONG can parse them.
        MemcachedClient numClient = proxyClient((proxy, method, args) -> {
            if ("get".equals(method.getName()) && args.length == 1 && args[0] instanceof Collection) {
                Map<String, Object> map = new HashMap<>();
                map.put("k1", 42L);
                return map;
            }
            return null;
        });
        var template = new XmemcachedOperationTemplate(numClient, null);
        Map<String, Long> result = template.mGetLong(List.of("k1"));
        assertNotNull(result);
        assertEquals(42L, result.get("k1"));
    }

    @Test
    void shouldMGetInteger() {
        MemcachedClient numClient = proxyClient((proxy, method, args) -> {
            if ("get".equals(method.getName()) && args.length == 1 && args[0] instanceof Collection) {
                Map<String, Object> map = new HashMap<>();
                map.put("k1", 7);
                return map;
            }
            return null;
        });
        var template = new XmemcachedOperationTemplate(numClient, null);
        Map<String, Integer> result = template.mGetInteger(List.of("k1"));
        assertNotNull(result);
        assertEquals(7, result.get("k1"));
    }

    // ----------------------------------------------------------- incr

    @Test
    void shouldIncr() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(1L, template.incr("k", 1));
    }

    @Test
    void shouldThrowForNegativeDeltaIncr() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incr("k", -1));
    }

    @Test
    void shouldIncrWithPositiveSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.incr("k", 1, 60));
    }

    @Test
    void shouldIncrWithNonPositiveSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.incr("k", 1, 0));
        assertEquals(10L, template.incr("k", 1, -5));
    }

    @Test
    void shouldIncrWithPositiveDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.incr("k", 1, Duration.ofSeconds(30)));
    }

    @Test
    void shouldIncrWithNegativeDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.incr("k", 1, Duration.ofSeconds(-1)));
    }

    @Test
    void shouldWrapExceptionInIncr() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incr("k", 1));
    }

    @Test
    void shouldWrapExceptionInIncrWithSeconds() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incr("k", 1, 60));
    }

    @Test
    void shouldWrapExceptionInIncrWithDuration() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incr("k", 1, Duration.ofSeconds(10)));
    }

    @Test
    void shouldThrowForNegativeDeltaIncrWithSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incr("k", -1, 60));
    }

    @Test
    void shouldThrowForNegativeDeltaIncrWithDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incr("k", -1, Duration.ofSeconds(10)));
    }

    // ----------------------------------------------------------- incrWithNoReply

    @Test
    void shouldIncrWithNoReply() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertDoesNotThrow(() -> template.incrWithNoReply("k", 1));
    }

    @Test
    void shouldThrowForNegativeDeltaIncrWithNoReply() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incrWithNoReply("k", -1));
    }

    @Test
    void shouldWrapExceptionInIncrWithNoReply() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.incrWithNoReply("k", 1));
    }

    // ----------------------------------------------------------- decr

    @Test
    void shouldDecr() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(1L, template.decr("k", 1));
    }

    @Test
    void shouldThrowForNegativeDeltaDecr() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decr("k", -1));
    }

    @Test
    void shouldDecrWithPositiveSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.decr("k", 1, 60));
    }

    @Test
    void shouldDecrWithNonPositiveSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.decr("k", 1, 0));
        assertEquals(10L, template.decr("k", 1, -5));
    }

    @Test
    void shouldDecrWithPositiveDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.decr("k", 1, Duration.ofSeconds(30)));
    }

    @Test
    void shouldDecrWithNegativeDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertEquals(10L, template.decr("k", 1, Duration.ofSeconds(-1)));
    }

    @Test
    void shouldWrapExceptionInDecr() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decr("k", 1));
    }

    @Test
    void shouldWrapExceptionInDecrWithSeconds() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decr("k", 1, 60));
    }

    @Test
    void shouldWrapExceptionInDecrWithDuration() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decr("k", 1, Duration.ofSeconds(10)));
    }

    @Test
    void shouldThrowForNegativeDeltaDecrWithSeconds() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decr("k", -1, 60));
    }

    @Test
    void shouldThrowForNegativeDeltaDecrWithDuration() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decr("k", -1, Duration.ofSeconds(10)));
    }

    // ----------------------------------------------------------- decrWithNoReply

    @Test
    void shouldDecrWithNoReply() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertDoesNotThrow(() -> template.decrWithNoReply("k", 1));
    }

    @Test
    void shouldThrowForNegativeDeltaDecrWithNoReply() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decrWithNoReply("k", -1));
    }

    @Test
    void shouldWrapExceptionInDecrWithNoReply() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.decrWithNoReply("k", 1));
    }

    // ----------------------------------------------------------- del

    @Test
    void shouldDeleteKeys() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertDoesNotThrow(() -> template.del("k1", "k2"));
    }

    @Test
    void shouldTolerateNullKeysOnDelete() {
        var template = new XmemcachedOperationTemplate(defaultClient(), null);
        assertDoesNotThrow(() -> template.del((String[]) null));
    }

    @Test
    void shouldWrapExceptionInDel() {
        var template = new XmemcachedOperationTemplate(failingClient(), null);
        assertThrows(XMemcachedOperationException.class, () -> template.del("k"));
    }
}
