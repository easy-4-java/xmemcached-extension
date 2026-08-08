package com.googlecode.xmemcached.extension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link XMemcachedOperationException}.
 *
 * <p>Verifies the constructors preserve their message and cause arguments so
 * the template can safely wrap arbitrary xmemcached failures.</p>
 *
 * @since 3.0.0
 */
class XMemcachedOperationExceptionTest {

    @Test
    void shouldPreserveMessageOnlyConstructor() {
        XMemcachedOperationException ex = new XMemcachedOperationException("boom");
        assertEquals("boom", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void shouldPreserveMessageAndCauseConstructor() {
        Throwable cause = new IllegalStateException("root");
        XMemcachedOperationException ex = new XMemcachedOperationException("outer", cause);

        assertEquals("outer", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void shouldAllowNullMessageAndCause() {
        XMemcachedOperationException ex = new XMemcachedOperationException(null, null);
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }
}
