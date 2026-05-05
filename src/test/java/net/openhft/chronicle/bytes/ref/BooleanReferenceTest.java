/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests {@link BinaryBooleanReference} and {@link TextBooleanReference}
 * binary and text formats because correct boolean encoding is required
 * to avoid flag corruption in protocol state management. This test validates
 * read, write, closed-state errors, and bytesStore reassignment behaviour.
 */
@SuppressWarnings({"MMOverusedWord", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Boolean reference binary and text format behaviours")
class BooleanReferenceTest extends BytesTestCommon {

    @BeforeEach
    void skipOnWindowsAndWsl() {
        // Skip on Windows/WSL due to JVM native crash (STATUS_HEAP_CORRUPTION)
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL due to JVM crash");
    }

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

    @Test
    @DisplayName("text boolean reference toString reports current value")
    public void textReferenceToStringReportsValue() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {
            TextBooleanReference.write(true, nbs, 0);
            ref.bytesStore(nbs, 0, ref.maxSize());
            assertEquals("value: true",
                    ref.toString(),
                    "toString should reflect the current boolean value");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text boolean reference toString handles closed state")
    public void textReferenceToStringWhenClosed() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        @NotNull TextBooleanReference ref = new TextBooleanReference();
        TextBooleanReference.write(false, nbs, 0);
        ref.bytesStore(nbs, 0, ref.maxSize());
        ref.close();
        String result = ref.toString();
        assertNotNull(result, "toString should return a non-null string when reference is closed");
        assertTrue(result.contains("Closed"),
                "toString result '" + result + "' should contain 'Closed'");
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text boolean reference getValue throws when closed")
    public void textReferenceGetValueThrowsWhenClosed() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        @NotNull TextBooleanReference ref = new TextBooleanReference();
        TextBooleanReference.write(true, nbs, 0);
        ref.bytesStore(nbs, 0, ref.maxSize());
        ref.close();
        assertThrows(ClosedIllegalStateException.class,
                ref::getValue,
                "text getValue should throw when reference is closed");
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text boolean reference setValue throws when closed")
    public void textReferenceSetValueThrowsWhenClosed() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        @NotNull TextBooleanReference ref = new TextBooleanReference();
        TextBooleanReference.write(true, nbs, 0);
        ref.bytesStore(nbs, 0, ref.maxSize());
        ref.close();
        assertThrows(ClosedIllegalStateException.class,
                () -> ref.setValue(false),
                "text setValue should throw when reference is closed");
        nbs.releaseLast();
    }

    @Test
    @DisplayName("binary boolean reference getValue throws when closed")
    public void binaryReferenceGetValueThrowsWhenClosed() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(1);
        @NotNull BinaryBooleanReference ref = new BinaryBooleanReference();
        nbs.writeByte(0, (byte) BinaryWireCode.TRUE);
        ref.bytesStore(nbs, 0, 1);
        ref.close();
        assertThrows(ClosedIllegalStateException.class,
                ref::getValue,
                "binary getValue should throw when reference is closed");
        nbs.releaseLast();
    }

    @Test
    @DisplayName("binary boolean reference setValue throws when closed")
    public void binaryReferenceSetValueThrowsWhenClosed() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(1);
        @NotNull BinaryBooleanReference ref = new BinaryBooleanReference();
        nbs.writeByte(0, (byte) BinaryWireCode.FALSE);
        ref.bytesStore(nbs, 0, 1);
        ref.close();
        assertThrows(ClosedIllegalStateException.class,
                () -> ref.setValue(true),
                "binary setValue should throw when reference is closed");
        nbs.releaseLast();
    }

    @Test
    @DisplayName("binary boolean reference bytesStore throws when closed")
    public void binaryReferenceBytesStoreThrowsWhenClosed() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(1);
        @NotNull BinaryBooleanReference ref = new BinaryBooleanReference();
        ref.close();
        assertThrows(ClosedIllegalStateException.class,
                () -> ref.bytesStore(nbs, 0, 1),
                "bytesStore should throw when reference is closed");
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text boolean reference writes false correctly")
    public void textReferenceWriteFalse() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {
            TextBooleanReference.write(false, nbs, 0);
            ref.bytesStore(nbs, 0, ref.maxSize());
            assertFalse(ref.getValue(),
                    "Text reference should read false after writing false");
            assertEquals("value: false",
                    ref.toString(),
                    "toString should report value equals false after writing false");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("binary boolean reference returns bytesStore correctly")
    public void binaryReferenceReturnsBytesStore() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(1);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            nbs.writeByte(0, (byte) BinaryWireCode.TRUE);
            ref.bytesStore(nbs, 0, 1);
            assertNotNull(ref.bytesStore(),
                    "binary bytesStore should return the underlying store");
            assertEquals(0, ref.offset(),
                    "binary offset should return the configured offset");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text boolean reference returns bytesStore correctly")
    public void textReferenceReturnsBytesStore() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {
            TextBooleanReference.write(true, nbs, 0);
            ref.bytesStore(nbs, 0, ref.maxSize());
            assertNotNull(ref.bytesStore(),
                    "text bytesStore should return the underlying store");
            assertEquals(0, ref.offset(),
                    "text offset should return the configured offset");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("binary boolean reference returns null bytesStore before initialisation")
    public void binaryReferenceNullBytesStoreBeforeInit() {
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            assertNull(ref.bytesStore(),
                    "binary bytesStore should return null before initialisation");
        }
    }

    @Test
    @DisplayName("text boolean reference returns null bytesStore before initialisation")
    public void textReferenceNullBytesStoreBeforeInit() {
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {
            assertNull(ref.bytesStore(),
                    "text bytesStore should return null before initialisation");
        }
    }

    @Test
    @DisplayName("binary boolean reference handles reassignment of bytesStore")
    public void binaryReferenceReassignBytesStore() {
        BytesStore<?, Void> nbs1 = BytesStore.nativeStoreWithFixedCapacity(1);
        BytesStore<?, Void> nbs2 = BytesStore.nativeStoreWithFixedCapacity(1);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            nbs1.writeByte(0, (byte) BinaryWireCode.TRUE);
            ref.bytesStore(nbs1, 0, 1);
            assertTrue(ref.getValue(),
                    "Initial bytesStore should report true");

            nbs2.writeByte(0, (byte) BinaryWireCode.FALSE);
            ref.bytesStore(nbs2, 0, 1);
            assertFalse(ref.getValue(),
                    "Reassigned bytesStore should report false");
        }
        nbs1.releaseLast();
        nbs2.releaseLast();
    }
}
