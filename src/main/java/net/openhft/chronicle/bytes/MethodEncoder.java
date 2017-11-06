package net.openhft.chronicle.bytes;

public interface MethodEncoder {
    long messageId();

    void encode(Object[] objects, BytesOut out);

    Object[] decode(Object[] lastObjects, BytesIn in);
}
