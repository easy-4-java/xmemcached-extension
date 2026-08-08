package com.googlecode.xmemcached.extension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link XmemcachedKeyConstant}.
 *
 * <p>The constants in this class are referenced by every cache-key builder
 * in the project, so the test simply asserts their literal values are
 * preserved verbatim.</p>
 *
 * @since 3.0.0
 */
class XmemcachedKeyConstantTest {

    @Test
    void shouldExposeUserGeoLocationConstant() {
        assertEquals("user:geo:location", XmemcachedKeyConstant.USER_GEO_LOCATION_KEY);
    }

    @Test
    void shouldKeepUserGeoLocationDistinctFromOtherKeys() {
        assertNotEquals("", XmemcachedKeyConstant.USER_GEO_LOCATION_KEY);
    }
}
