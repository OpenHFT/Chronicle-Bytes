package net.openhft.chronicle.bytes;

@FunctionalInterface
public interface BytesParselet {
    void accept(long messageType, BytesIn in);
}
