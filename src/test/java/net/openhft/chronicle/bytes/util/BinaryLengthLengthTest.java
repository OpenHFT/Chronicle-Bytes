/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.jupiter.api.Assertions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class BinaryLengthLengthTest extends BytesTestCommon {

    private final BinaryLengthLength binaryLengthLength;
    private final int binaryWireCode;

    public BinaryLengthLengthTest(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        this.binaryLengthLength = binaryLengthLength;
        this.binaryWireCode = binaryWireCode;
    }

    @Parameterized.Parameters(name = "binaryLengthLength {0} binaryWireCode {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BinaryLengthLength.LENGTH_8BIT, BinaryWireCode.BYTES_LENGTH8},
                {BinaryLengthLength.LENGTH_16BIT, BinaryWireCode.BYTES_LENGTH16},
                {BinaryLengthLength.LENGTH_32BIT, BinaryWireCode.BYTES_LENGTH32}
        });
    }

    @Before
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void testInvalidLengthFor8Bit() {
        BytesOut<?> bytes = Bytes.allocateDirect(512);
        long pos = BinaryLengthLength.LENGTH_8BIT.initialise(bytes);
        bytes.writeSkip(256);
        Assertions.assertThrows(IllegalStateException.class, () -> BinaryLengthLength.LENGTH_8BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()));
        bytes.releaseLast();
    }

    @Test
    public void testInvalidLengthFor16Bit() {
        BytesOut<?> bytes = Bytes.allocateDirect(65539);
        long pos = BinaryLengthLength.LENGTH_16BIT.initialise(bytes);
        bytes.writeSkip(65536);
        Assertions.assertThrows(IllegalStateException.class, () -> BinaryLengthLength.LENGTH_16BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()));
        bytes.releaseLast();
    }

    @Test
    public void checkCodeMatches() {
        assertEquals(binaryWireCode, binaryLengthLength.code());
    }

    @Test
    public void checkCodeIsWritten() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128);
        binaryLengthLength.initialise(bytes);
        byte readCode = (byte) bytes.readUnsignedByte();
        assertEquals((byte) binaryWireCode, readCode);
        bytes.releaseLast();
    }
}
