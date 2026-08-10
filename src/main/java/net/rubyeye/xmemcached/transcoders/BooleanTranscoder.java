package net.rubyeye.xmemcached.transcoders;

/**
 * Transcoder for {@link Boolean} values, supporting both string-mode and
 * raw-bytes serialisation.
 *
 * <p>When {@link #setPrimitiveAsString(boolean) primitiveAsString} is
 * enabled the boolean is encoded as its {@code toString()} representation
 * (e.g. {@code "true"} / {@code "false"}) so it can interoperate with
 * non-Java memcached clients. Otherwise the transcoder delegates to the
 * {@link TranscoderUtils} helper which packs the value as a single byte
 * tagged with {@link SerializingTranscoder#SPECIAL_BOOLEAN}.</p>
 *
 * <p>Decoding is symmetric: when the stored flag is {@code 0} the
 * transcoder interprets the bytes as a UTF-8 string, and when the flag
 * matches {@link SerializingTranscoder#SPECIAL_BOOLEAN} it parses the
 * single-byte representation. Any other flag causes {@code decode} to
 * return {@code null}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see PrimitiveTypeTranscoder
 * @see SerializingTranscoder
 */
public class BooleanTranscoder extends PrimitiveTypeTranscoder<Boolean> {

    /**
     * Encode a {@link Boolean} using the currently configured serialisation
     * strategy.
     *
     * @param l the boolean to encode; must not be {@code null}
     * @return a non-{@code null} {@link CachedData} containing either the
     *         string-encoded payload or the special-flagged single-byte
     *         representation
     */
    @Override
    public CachedData encode(Boolean l) {
        // store integer as string
        if (this.primitiveAsString) {
            byte[] b = encodeString(l.toString());
            int flags = 0;
            if (b.length > this.compressionThreshold) {
                byte[] compressed = compress(b);
                if (compressed.length < b.length) {
                    if (log.isDebugEnabled()) {
                        log.debug("Compressed " + l.getClass().getName() + " from " + b.length + " to "
                                + compressed.length);
                    }
                    b = compressed;
                    flags |= SerializingTranscoder.COMPRESSED;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Compression increased the size of " + l.getClass().getName() + " from "
                                + b.length + " to " + compressed.length);
                    }
                }
            }
            return new CachedData(flags, b, b.length, -1);
        }
        return new CachedData(SerializingTranscoder.SPECIAL_BOOLEAN, this.tu.encodeBoolean(l));
    }

    /**
     * Decode the supplied {@link CachedData} back into a {@link Boolean}
     * using the inverse of the encoding strategy.
     *
     * @param d the cache entry to decode; must not be {@code null}
     * @return the decoded boolean, or {@code null} when the stored flag
     *         does not match the expected encoding
     */
    @Override
    public Boolean decode(CachedData d) {
        if (this.primitiveAsString) {
            byte[] data = d.getData();
            if ((d.getFlag() & SerializingTranscoder.COMPRESSED) != 0) {
                data = decompress(d.getData());
            }
            int flag = d.getFlag();
            if (flag == 0) {
                return Boolean.valueOf(decodeString(data));
            } else {
                return null;
            }
        } else {
            if (SerializingTranscoder.SPECIAL_BOOLEAN == d.getFlag()) {
                return this.tu.decodeBoolean(d.getData());
            } else {
                return null;
            }
        }
    }

    /**
     * Toggle whether booleans are serialised as strings.
     *
     * @param primitiveAsString when {@code true} the encoder uses the
     *                          string-based layout and the decoder expects
     *                          the same layout
     */
    @Override
    public void setPrimitiveAsString(boolean primitiveAsString) {
        this.primitiveAsString = primitiveAsString;
    }

    /**
     * Toggle pack-zeros mode on the underlying
     * {@link TranscoderUtils} helper.
     *
     * @param packZeros the new pack-zeros flag forwarded to
     *                  {@link TranscoderUtils#setPackZeros(boolean)}
     */
    @Override
    public void setPackZeros(boolean packZeros) {
        this.tu.setPackZeros(packZeros);
    }

}
