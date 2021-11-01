package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.releaseAndAssertReleased;
import static net.openhft.chronicle.bytes.BytesFactoryUtil.wipe;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Not ready yet")
final class BytesReleaseInvariantTest extends BytesTestCommon {

    private static final int SIZE = 128;

    /**
     * Checks a released Bytes handles toString safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equals(final Bytes<?> bytes) {
        releaseAndAssertReleased(bytes);
        assertDoesNotThrow(() -> {
            bytes.writeChar('A');
            final Bytes<?> other = Bytes.from("A");
            try {
                final boolean equals = bytes.equals(other);
            } finally {
                other.releaseLast();
            }
        }, bytes.getClass().getName());
    }

    /**
     * Checks a released Bytes handles toString safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void hashcode(final Bytes<?> bytes) {
        releaseAndAssertReleased(bytes);
        assertDoesNotThrow(() -> {
            bytes.writeChar('A');
            final Bytes<?> other = Bytes.from("A");
            final int expected = other.hashCode();
            try {

                final int actutal = bytes.hashCode();
                assertEquals(expected, actutal);
            } finally {
                other.releaseLast();
            }
        }, bytes.getClass().getName());
    }

    // Todo: Release in a separate thread.

    @Test
    void contract() {
        // There should exist no combination of method invocation that can make the JVM crash.

        char bullet = 'A';
        for (String validity :"of,of valid".split(",")) {
            for (String threadConfinement:"across threads,within the same thread".split(",")) {
                for (String problem:"make the JVM crash,put the object in an illegal state".split(",")) {
                    for (String unless : ",unless documented".split(",")) {
                        System.out.format("%c) There should exist no combination %s method invocations %s that can %s %s%n",
                                bullet++, validity, threadConfinement, problem, unless);
                    }
                }
            }
        }
    }

    /**
     * Checks a released Bytes handles toString safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void toString(final Bytes<?> bytes) {
        releaseAndAssertReleased(bytes);
        assertDoesNotThrow(() -> {
            String toString = bytes.toString();
        }, bytes.getClass().getName());
    }

    @Test
    void stringBuilderTest() {
        StringBuilder sb = new StringBuilder("A");
        System.out.println("sb.hashCode() = " + sb.hashCode());
        System.out.println("System.identityHashCode(sb) = " + System.identityHashCode(sb));
        System.out.println("sb = " + sb);
    }

    @Test
    void manualTest() {
        Bytes<?> bytes = wipe(Bytes.allocateElasticDirect());
        releaseAndAssertReleased(bytes);
        bytes.toString();
    }

}