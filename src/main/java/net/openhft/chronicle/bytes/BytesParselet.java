package net.openhft.chronicle.bytes;

@FunctionalInterface
public interface BytesParselet {
    @SuppressWarnings("rawtypes")
    void accept(long messageType, BytesIn in);
}
