/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

public class BooleanReferenceTest extends BytesTestCommon {
    @Test
    public void testBinary() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(2);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            // First value
            byte val1 = (byte) BinaryWireCode.FALSE;
            nbs.writeByte(0, val1);

            ref.bytesStore(nbs, 0, 1);

            assertFalse(ref.getValue(), "BinaryBooleanReference should return false when backed by FALSE wire code at offset 0");
            ref.setValue(true);

            // Second value
            byte val2 = (byte) BinaryWireCode.TRUE; // true
            nbs.writeByte(1, val2);

            ref.bytesStore(nbs, 1, 1);
            assertTrue(ref.getValue(), "BinaryBooleanReference should return true when backed by TRUE wire code at offset 1");
            assertEquals(1, ref.maxSize(), "BinaryBooleanReference maxSize should be 1 byte for binary wire format");

        }
        nbs.releaseLast();
    }

    @Test
    public void testText() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {

            // First value
            nbs.write(0, "false".getBytes(ISO_8859_1));

            ref.bytesStore(nbs, 0, 5);

            assertFalse(ref.getValue(), "TextBooleanReference should return false when backed by 'false' text at offset 0");
            ref.setValue(true);

            // Second value
            nbs.write(5, " true".getBytes(ISO_8859_1));

            ref.bytesStore(nbs, 5, 5);
            assertTrue(ref.getValue(), "TextBooleanReference should return true when backed by ' true' text at offset 5");
            assertEquals(5, ref.maxSize(), "TextBooleanReference maxSize should be 5 bytes for text format");

        }
        nbs.releaseLast();
    }
}
