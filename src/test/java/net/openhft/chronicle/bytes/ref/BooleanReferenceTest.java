/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BooleanReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("binary boolean reference reads and writes true/false")
    public void testBinary() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(2);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            // First value
            byte val1 = (byte) BinaryWireCode.FALSE;
            nbs.writeByte(0, val1);

            ref.bytesStore(nbs, 0, 1);

            assertFalse(ref.getValue(),
                    "Binary reference should read false from initial value");
            ref.setValue(true);

            // Second value
            byte val2 = (byte) BinaryWireCode.TRUE; // true
            nbs.writeByte(1, val2);

            ref.bytesStore(nbs, 1, 1);
            assertTrue(ref.getValue(),
                    "Binary reference should read true from updated value");
            assertEquals(1,
                    ref.maxSize(),
                    "Binary reference should report a one-byte max size");

        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("binary boolean reference rejects invalid lengths")
    public void binaryRejectsInvalidLength() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(2);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            assertThrows(IllegalArgumentException.class,
                    () -> ref.bytesStore(nbs, 0, 2),
                    "Binary boolean reference should reject lengths larger than one byte");
        } finally {
            nbs.releaseLast();
        }
    }

    @Test
    @DisplayName("binary boolean reference throws on unexpected byte codes")
    public void binaryThrowsOnUnexpectedByte() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(1);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            nbs.writeByte(0, (byte) 0x00);
            ref.bytesStore(nbs, 0, 1);
            assertThrows(IllegalStateException.class,
                    ref::getValue,
                    "Unexpected byte values should be rejected");
        } finally {
            nbs.releaseLast();
        }
    }

    @Test
    @DisplayName("binary boolean reference masks offsets for hex dump bytes")
    public void binaryMasksHexDumpOffsets() {
        HexDumpBytes bytes = new HexDumpBytes();
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            bytes.writeByte(0, (byte) BinaryWireCode.TRUE);
            long maskedOffset = (1L << 32) | 0L;
            ref.bytesStore(bytes, maskedOffset, 1);
            assertTrue(ref.getValue(),
                    "Hex dump offsets should be masked to the underlying byte offset");
            ref.setValue(false);
            assertEquals((byte) BinaryWireCode.FALSE,
                    bytes.readByte(0),
                    "Masked offset writes should update the underlying bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("text boolean reference reads and writes true/false")
    public void testText() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {

            // First value
            nbs.write(0, "false".getBytes(StandardCharsets.ISO_8859_1));

            ref.bytesStore(nbs, 0, 5);

            assertFalse(ref.getValue(),
                    "Text reference should read false from initial value");
            ref.setValue(true);

            // Second value
            nbs.write(5, " true".getBytes(StandardCharsets.ISO_8859_1));

            ref.bytesStore(nbs, 5, 5);
            assertTrue(ref.getValue(),
                    "Text reference should read true from updated value");
            assertEquals(5,
                    ref.maxSize(),
                    "Text reference should report the expected width");

        }
        nbs.releaseLast();
    }
}
