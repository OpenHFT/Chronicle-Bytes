package net.openhft.chronicle.bytes;

@FunctionalInterface
public interface OffsetFormat {
    @SuppressWarnings("rawtypes")
    void append(long offset, Bytes bytes);
}
