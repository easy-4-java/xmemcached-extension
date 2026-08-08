package net.rubyeye.xmemcached.transcoders;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link CustomTypeTranscoder}.
 *
 * @since 3.0.0
 */
class CustomTypeTranscoderTest {

    @Test
    void shouldEncodePayloadUsingToStringRepresentation() {
        CustomTypeTranscoder<Integer> transcoder = new CustomTypeTranscoder<>();

        CachedData data = transcoder.encode(42);

        assertNotNull(data);
        assertEquals("42", new String(data.getData(), StandardCharsets.UTF_8));
        assertEquals(2, data.getSize());
        // CAS field is intentionally set to -1.
        assertEquals(-1L, data.getCas());
    }

    @Test
    void shouldSkipCompressionBelowThreshold() {
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();
        transcoder.setCompressionThreshold(Integer.MAX_VALUE);

        CachedData data = transcoder.encode("hello");

        assertEquals(0, data.getFlag());
        assertEquals("hello", new String(data.getData(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldCompressPayloadAboveThreshold() {
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();
        transcoder.setCompressionThreshold(2);

        String payload = "this is a fairly long payload that should be compressed by the transcoder";
        CachedData data = transcoder.encode(payload);

        assertNotNull(data);
        // The COMPRESSED flag must be set because compression yielded savings.
        assertEquals(SerializingTranscoder.COMPRESSED, data.getFlag());
        assertEquals(payload.length(), data.getSize());
    }

    @Test
    void shouldNotCompressWhenCompressionDoesNotShrinkPayload() {
        // A 3-byte payload below the threshold should not be touched.
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();
        transcoder.setCompressionThreshold(Integer.MAX_VALUE);

        CachedData data = transcoder.encode("abc");

        assertEquals(0, data.getFlag());
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), data.getData());
    }

    @Test
    void shouldAlwaysReturnNullOnDecode() {
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();

        assertNull(transcoder.decode(new CachedData(0, "x".getBytes(StandardCharsets.UTF_8))));
        assertNull(transcoder.decode(null));
    }

    @Test
    void shouldAlwaysReportPrimitiveFlagsFalse() {
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();

        assertFalse(transcoder.isPrimitiveAsString());
        assertFalse(transcoder.isPackZeros());
    }

    @Test
    void shouldIgnorePrimitiveAndPackZeroSetters() {
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();

        transcoder.setPrimitiveAsString(true);
        transcoder.setPackZeros(true);

        assertFalse(transcoder.isPrimitiveAsString());
        assertFalse(transcoder.isPackZeros());
    }

    @Test
    void shouldExposeBaseSerializingTranscoderConfiguration() {
        CustomTypeTranscoder<String> transcoder = new CustomTypeTranscoder<>();
        transcoder.setCharset("UTF-8");

        // Should not throw; just exercises the inherited surface.
        assertNotNull(transcoder.getCompressMode());
    }
}
