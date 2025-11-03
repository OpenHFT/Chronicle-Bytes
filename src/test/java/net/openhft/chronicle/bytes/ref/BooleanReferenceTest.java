/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class BooleanReferenceTest extends BytesTestCommon {
    @Test
    public void testBinary() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(2);
        try (@NotNull BinaryBooleanReference ref = new BinaryBooleanReference()) {
            // First value
            byte val1 = (byte) BinaryWireCode.FALSE;
            nbs.writeByte(0, val1);

            ref.bytesStore(nbs, 0, 1);

            assertFalse(ref.getValue());
            ref.setValue(true);

            // Second value
            byte val2 = (byte) BinaryWireCode.TRUE; // true
            nbs.writeByte(1, val2);

            ref.bytesStore(nbs, 1, 1);
            assertTrue(ref.getValue());
            assertEquals(1, ref.maxSize());

        }
        nbs.releaseLast();
    }

    @Test
    public void testText() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(5);
        try (@NotNull TextBooleanReference ref = new TextBooleanReference()) {

            // First value
            nbs.write(0, "false".getBytes(StandardCharsets.ISO_8859_1));

            ref.bytesStore(nbs, 0, 5);

            assertFalse(ref.getValue());
            ref.setValue(true);

            // Second value
            nbs.write(5, " true".getBytes(StandardCharsets.ISO_8859_1));

            ref.bytesStore(nbs, 5, 5);
            assertTrue(ref.getValue());
            assertEquals(5, ref.maxSize());

        }
        nbs.releaseLast();
    }
}
