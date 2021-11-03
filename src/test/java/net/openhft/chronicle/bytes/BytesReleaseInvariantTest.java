package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BytesReleaseInvariantTest extends BytesTestCommon {

    /**
     * Checks a released Bytes handles "equals()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equals(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        if (readWrite) {
            bytes.writeChar('A');
        }
        releaseAndAssertReleased(bytes);
        final Bytes<?> other = Bytes.from("A");
        try {
            assertThrows(ClosedIllegalStateException.class, () -> bytes.equals(other), createCommand);
        } finally {
            other.releaseLast();
        }
    }

    /**
     * Checks a released Bytes handles "hashCode()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void hashcode(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        releaseAndAssertReleased(bytes);
        assertThrows(ClosedIllegalStateException.class, bytes::hashCode, createCommand);
    }

    // Todo: Release in a separate thread.

    @Test
    void contract() {
        // There should exist no combination of method invocation that can make the JVM crash.

        char bullet = 'A';
        for (String validity : "of,of valid".split(",")) {
            for (String threadConfinement : "across threads,within the same thread".split(",")) {
                for (String problem : "make the JVM crash,put the object in an illegal state".split(",")) {
                    for (String unless : ",unless documented".split(",")) {
                        System.out.format("%c) There should exist no combination %s method invocations %s that can %s %s%n",
                                bullet++, validity, threadConfinement, problem, unless);
                    }
                }
            }
        }
    }

    /**
     * Checks a released Bytes handles "toString()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void toString(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        final String expected;
        if (readWrite) {
            expected = "The quick brown fox jumped over the usual suspect.";
            bytes.append(expected);
        } else {
            expected = "";
        }
        final String toString = bytes.toString();
        assertEquals(expected, toString);
        releaseAndAssertReleased(bytes);
        assertThrows(ClosedIllegalStateException.class, bytes::toString, createCommand);
    }

    //@Test
    void manualTest() {
/*        provideBytesObjects()
                .map(BytesFactoryUtil::bytes)
                .filter(bytes -> bytes.getClass().getSimpleName().contains("Unchecked"))
                .forEach(bytes -> {
                    bytes.append("Arne");
                    releaseAndAssertReleased(bytes);
                    bytes.toString();
                });*/

        Bytes<?> bytes = wipe(Bytes.allocateDirect(SIZE).unchecked(true));
        bytes.append("Arne");
        releaseAndAssertReleased(bytes);
        bytes.toString();

    }

}