package net.openhft.chronicle.bytes;

public interface MethodEncoder {
    long messageId();

    @SuppressWarnings("rawtypes")
    void encode(Object[] objects, BytesOut out);

    @SuppressWarnings("rawtypes")
    Object[] decode(Object[] lastObjects, BytesIn in);
}
