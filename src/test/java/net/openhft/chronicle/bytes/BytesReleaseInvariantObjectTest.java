/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.junit.jupiter.api.DisplayName;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.releaseAndAssertReleased;
import static net.openhft.chronicle.bytes.BytesFactoryUtil.wipe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that object methods like equals, hashCode, and toString handle closed buffers correctly
 * because safe failure is essential for detecting resource management errors.
 */
@DisplayName("Closed buffer object method safety behaviour")
final class BytesReleaseInvariantObjectTest extends BytesTestCommon {

    /**
     * Checks a closed buffer handles "equals()" safely
     */
    @ParameterizedTest
    @DisplayName("equals operation throws ClosedIllegalStateException after resource disposal")
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void equalsContentAffects(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        if (readWrite) {
            bytes.writeChar('A');
        }
        final Bytes<?> other = Bytes.from("A");
        try {
            releaseAndAssertReleased(bytes);
            final Executable task = () -> bytes.equals(other);
            assertThrows(ClosedIllegalStateException.class,
                    task,
                    "Disposed buffer should reject equals when created via " + createCommand);
        } finally {
            other.releaseLast();
        }
    }

    /**
     * Checks a disposed resource handles "hashCode()" safely
     */
    @ParameterizedTest
    @DisplayName("hashCode operation throws ClosedIllegalStateException after resource disposal")
    @MethodSource("net.openhft.chronicle.bytes.BytesFactoryUtil#provideBytesObjects")
    void hashcodeContentAffect(final Bytes<?> bytes, final boolean readWrite, final String createCommand) {
        releaseAndAssertReleased(bytes);
        final Executable task = bytes::hashCode;
        assertThrows(ClosedIllegalStateException.class,
                task,
                "Disposed buffer should reject hashCode when created via " + createCommand);

    }

    /**
     * Checks a disposed resource handles "toString()" safely
     */
    @ParameterizedTest
    @DisplayName("toString shows content before resource disposal")
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
        assertEquals(expected,
                toString,
                "toString should reflect content before disposal for " + createCommand);
        releaseAndAssertReleased(bytes);
        String hexString = bytes.toHexString();
        if (!hexString.startsWith("net.openhft.chronicle.core.io.ClosedIllegalStateException")) {
            assertEquals("net.openhft.chronicle.core.io.ClosedIllegalStateException: net.openhft.chronicle.bytes.NativeBytes already released INIT location ",
                    hexString,
                    "Disposed buffer should report the expected exception message for " + createCommand);
        }
    }
}
