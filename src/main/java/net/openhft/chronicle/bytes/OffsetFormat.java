package net.openhft.chronicle.bytes;

@FunctionalInterface
public interface OffsetFormat {
    void append(long offset, Bytes bytes);
}
