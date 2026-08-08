package com.googlecode.xmemcached.extension.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Strings}.
 *
 * <p>Exercises every branch of {@link Strings#hasText(CharSequence)} so the
 * helper can be safely relied on by the cache-key builders in
 * {@link com.googlecode.xmemcached.extension.XmemcachedKey}.</p>
 *
 * @since 3.0.0
 */
class StringsTest {

    @Test
    void shouldReturnFalseWhenValueIsNull() {
        assertFalse(Strings.hasText(null));
    }

    @Test
    void shouldReturnFalseWhenValueIsEmpty() {
        assertFalse(Strings.hasText(""));
    }

    @Test
    void shouldReturnFalseWhenValueContainsOnlyWhitespace() {
        assertFalse(Strings.hasText("   "));
        assertFalse(Strings.hasText("\t\n  "));
    }

    @Test
    void shouldReturnTrueWhenValueContainsAtLeastOneNonWhitespaceChar() {
        assertTrue(Strings.hasText("a"));
        assertTrue(Strings.hasText("  hello  "));
        assertTrue(Strings.hasText("x"));
    }

    @Test
    void shouldAcceptCharSequenceImplementations() {
        StringBuilder sb = new StringBuilder("payload");
        assertTrue(Strings.hasText(sb));
    }
}
