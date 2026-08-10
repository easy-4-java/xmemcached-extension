package net.rubyeye.xmemcached.transcoders;

/**
 * A minimal transcoder that encodes an arbitrary object by serialising its
 * {@code toString()} representation.
 *
 * <p>{@link CustomTypeTranscoder} extends {@link BaseSerializingTranscoder}
 * so it inherits the project's compression machinery: payloads whose
 * encoded byte length exceeds {@code compressionThreshold} are compressed
 * via {@link BaseSerializingTranscoder#compress(byte[])} and flagged with
 * {@link SerializingTranscoder#COMPRESSED}.</p>
 *
 * <p>Note that {@link #decode(CachedData)} always returns {@code null}; this
 * transcoder is intended for one-way write paths (e.g. logging or
 * instrumentation) where round-tripping the value is unnecessary.</p>
 *
 * @param <T> value type handled by this transcoder
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see BaseSerializingTranscoder
 * @see Transcoder
 */
public class CustomTypeTranscoder<T> extends BaseSerializingTranscoder implements Transcoder<T> {

    /**
     * Encode {@code l} as a UTF-8 string of its {@code toString()}
     * representation, applying compression when warranted.
     *
     * @param l the value to encode; must not be {@code null}
     * @return a non-{@code null} {@link CachedData} containing the encoded
     *         payload; the {@link CachedData#getCas() CAS} field is
     *         intentionally set to {@code -1}
     */
    @Override
    public CachedData encode(T l) {
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

    /**
     * Decode the supplied {@link CachedData}. This implementation always
     * returns {@code null} because the transcoder does not retain enough
     * information to recover the original type.
     *
     * @param d the cache entry to decode; ignored
     * @return always {@code null}
     */
    @Override
    public T decode(CachedData d) {
        return null;
    }

    /**
     * Toggle whether primitive values are serialised as strings. This
     * transcoder does not deal in primitives; the call is a no-op kept to
     * satisfy the {@link Transcoder} contract.
     *
     * @param primitiveAsString ignored
     */
    @Override
    public void setPrimitiveAsString(boolean primitiveAsString) {
    }

    /**
     * Toggle pack-zeros mode. This transcoder does not deal in primitives;
     * the call is a no-op kept to satisfy the {@link Transcoder} contract.
     *
     * @param packZeros ignored
     */
    @Override
    public void setPackZeros(boolean packZeros) {
    }

    /**
     * Report whether primitive values are serialised as strings. Always
     * {@code false} for this transcoder.
     *
     * @return always {@code false}
     */
    @Override
    public boolean isPrimitiveAsString() {
        return false;
    }

    /**
     * Report whether pack-zeros mode is active. Always {@code false} for
     * this transcoder.
     *
     * @return always {@code false}
     */
    @Override
    public boolean isPackZeros() {
        return false;
    }

}
