package net.openhft.chronicle.bytes.internal;

public interface DecimalAppender {
    void append(boolean negative, long mantissa, int exponent);

    default void appendHighPrecision(double d) {
        throw new UnsupportedOperationException("d: " + d);
    }

    default void appendHighPrecision(float f) {
        throw new UnsupportedOperationException("f: " + f);
    }
}
