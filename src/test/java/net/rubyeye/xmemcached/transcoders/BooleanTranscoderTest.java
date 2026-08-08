package net.rubyeye.xmemcached.transcoders;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BooleanTranscoder}.
 *
 * @since 3.0.0
 */
class BooleanTranscoderTest {

    @Test
    void shouldEncodeBooleanAsStringWhenFlagEnabled() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        transcoder.setPrimitiveAsString(true);
        transcoder.setCompressionThreshold(Integer.MAX_VALUE);

        CachedData trueData = transcoder.encode(Boolean.TRUE);
        assertEquals("true", new String(trueData.getData(), StandardCharsets.UTF_8));
        assertEquals(0, trueData.getFlag());

        CachedData falseData = transcoder.encode(Boolean.FALSE);
        assertEquals("false", new String(falseData.getData(), StandardCharsets.UTF_8));
        assertEquals(0, falseData.getFlag());
    }

    @Test
    void shouldEncodeBooleanAsSingleByteByDefault() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        // primitiveAsString defaults to false.

        CachedData data = transcoder.encode(Boolean.TRUE);
        assertEquals(SerializingTranscoder.SPECIAL_BOOLEAN, data.getFlag());
        assertEquals(1, data.getData().length);
    }

    @Test
    void shouldDecodeStringEncodedBooleanWhenFlagEnabled() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        transcoder.setPrimitiveAsString(true);

        CachedData trueData = new CachedData(0, "true".getBytes(StandardCharsets.UTF_8));
        assertEquals(Boolean.TRUE, transcoder.decode(trueData));

        CachedData falseData = new CachedData(0, "false".getBytes(StandardCharsets.UTF_8));
        assertEquals(Boolean.FALSE, transcoder.decode(falseData));
    }

    @Test
    void shouldReturnNullWhenDecodingStringEntryWithNonZeroFlag() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        transcoder.setPrimitiveAsString(true);

        CachedData data = new CachedData(SerializingTranscoder.COMPRESSED, "true".getBytes(StandardCharsets.UTF_8));
        assertNull(transcoder.decode(data));
    }

    @Test
    void shouldDecodeSpecialBooleanWhenFlagMatches() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        // primitiveAsString is false.

        CachedData trueData = new CachedData(SerializingTranscoder.SPECIAL_BOOLEAN,
                new byte[] { 'T' });
        assertEquals(Boolean.TRUE, transcoder.decode(trueData));

        CachedData falseData = new CachedData(SerializingTranscoder.SPECIAL_BOOLEAN,
                new byte[] { 'F' });
        assertEquals(Boolean.FALSE, transcoder.decode(falseData));
    }

    @Test
    void shouldReturnNullWhenSpecialFlagDoesNotMatch() {
        BooleanTranscoder transcoder = new BooleanTranscoder();

        CachedData data = new CachedData(SerializingTranscoder.SPECIAL_INT,
                new byte[] { 'T' });
        assertNull(transcoder.decode(data));
    }

    @Test
    void shouldTogglePrimitiveAsStringFlag() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        assertFalse(transcoder.isPrimitiveAsString());

        transcoder.setPrimitiveAsString(true);
        assertTrue(transcoder.isPrimitiveAsString());
    }

    @Test
    void shouldDelegatePackZerosToTranscoderUtils() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        assertFalse(transcoder.isPackZeros());

        transcoder.setPackZeros(true);
        assertTrue(transcoder.isPackZeros());

        transcoder.setPackZeros(false);
        assertFalse(transcoder.isPackZeros());
    }

    @Test
    void shouldRoundTripTrueValue() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        CachedData encoded = transcoder.encode(Boolean.TRUE);
        assertEquals(Boolean.TRUE, transcoder.decode(encoded));
    }

    @Test
    void shouldRoundTripFalseValue() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        CachedData encoded = transcoder.encode(Boolean.FALSE);
        assertEquals(Boolean.FALSE, transcoder.decode(encoded));
    }

    @Test
    void shouldRoundTripAcrossStringMode() {
        BooleanTranscoder transcoder = new BooleanTranscoder();
        transcoder.setPrimitiveAsString(true);

        CachedData encoded = transcoder.encode(Boolean.TRUE);
        assertEquals(Boolean.TRUE, transcoder.decode(encoded));

        CachedData encodedFalse = transcoder.encode(Boolean.FALSE);
        assertEquals(Boolean.FALSE, transcoder.decode(encodedFalse));
    }
}
