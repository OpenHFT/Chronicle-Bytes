/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.releaseAndAssertReleased;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BytesReleaseInvariantObjectTest extends BytesTestCommon {

    /**
     * Checks a released Bytes handles "equals()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equalsContentAffects(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        if (readWrite) {
            bytes.writeChar('A');
        }
        final Bytes<?> other = Bytes.from("A");
        try {
            releaseAndAssertReleased(bytes);
            final Executable task = () -> bytes.equals(other);
            assertThrows(ClosedIllegalStateException.class, task, createCommand);
        } finally {
            other.releaseLast();
        }
    }

    /**
     * Checks a released Bytes handles "hashCode()" safely
     */
    @ParameterizedTest
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void hashcodeContentAffect(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        releaseAndAssertReleased(bytes);
        final Executable task = bytes::hashCode;
        assertThrows(ClosedIllegalStateException.class, task, createCommand);

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
        String hexString = bytes.toHexString();
        if (!hexString.startsWith("net.openhft.chronicle.core.io.ClosedIllegalStateException")) {
            assertEquals("net.openhft.chronicle.core.io.ClosedIllegalStateException: net.openhft.chronicle.bytes.NativeBytes already released INIT location ", hexString);
        }
    }
}
