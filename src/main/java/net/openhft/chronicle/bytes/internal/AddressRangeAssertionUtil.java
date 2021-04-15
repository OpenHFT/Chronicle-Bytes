package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.annotation.NonNegative;

public final class AddressRangeAssertionUtil {

    private AddressRangeAssertionUtil() {
    }

    public static boolean isInRange(@NonNegative long index,
                                    @NonNegative long endExclusive) {
        if (index < 0) {
            throw new AssertionError(
                    String.format("The provided index (%d) is negative", index)
            );
        }
        if (endExclusive < 0) {
            throw new AssertionError(
                    String.format("The provided endExclusive (%d) is negative", endExclusive)
            );
        }
        if (index >= endExclusive) {
            throw new AssertionError(
                    String.format("The provided index (%d) was equal or greater than the provided endExclusive (%d)", index, endExclusive)
            );
        }
        return true;
    }

    public static boolean isInRange(@NonNegative long index,
                                    @NonNegative long endExclusive,
                                    @NonNegative long reserving) {
        if (index < 0) {
            throw new AssertionError(
                    String.format("The provided index (%d) is negative", index)
            );
        }
        if (endExclusive < 0) {
            throw new AssertionError(
                    String.format("The provided endExclusive (%d) is negative", endExclusive)
            );
        }
        if (reserving < 0) {
            throw new AssertionError(
                    String.format("The provided reserving (%d) is negative", reserving)
            );
        }
        if (index > (endExclusive - reserving)) {
            throw new AssertionError(
                    String.format("The provided index (%d) was greater than (%d (= the provided endExclusive (%d) - the provided reserving (%d))", index, endExclusive - reserving, endExclusive, reserving)
            );
        }
        return true;
    }

}