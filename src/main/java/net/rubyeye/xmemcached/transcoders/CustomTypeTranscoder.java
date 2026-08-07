package net.rubyeye.xmemcached.transcoders;

public class CustomTypeTranscoder<T> extends BaseSerializingTranscoder implements Transcoder<T> {

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

    @Override
    public T decode(CachedData d) {
        return null;
    }

    @Override
    public void setPrimitiveAsString(boolean primitiveAsString) {
    }

    @Override
    public void setPackZeros(boolean packZeros) {
    }

    @Override
    public boolean isPrimitiveAsString() {
        return false;
    }

    @Override
    public boolean isPackZeros() {
        return false;
    }

}