/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BinaryLengthLengthTest extends BytesTestCommon {

    private BinaryLengthLength binaryLengthLength;
    private int binaryWireCode;

    public void initBinaryLengthLengthTest(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        this.binaryLengthLength = binaryLengthLength;
        this.binaryWireCode = binaryWireCode;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BinaryLengthLength.LENGTH_8BIT, BinaryWireCode.BYTES_LENGTH8},
                {BinaryLengthLength.LENGTH_16BIT, BinaryWireCode.BYTES_LENGTH16},
                {BinaryLengthLength.LENGTH_32BIT, BinaryWireCode.BYTES_LENGTH32}
        });
    }

    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    public void testInvalidLengthFor8Bit(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        initBinaryLengthLengthTest(binaryLengthLength, binaryWireCode);
        BytesOut<?> bytes = Bytes.allocateDirect(512);
        long pos = BinaryLengthLength.LENGTH_8BIT.initialise(bytes);
        bytes.writeSkip(256);
        assertThrows(IllegalStateException.class, () -> BinaryLengthLength.LENGTH_8BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()));
        bytes.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    public void testInvalidLengthFor16Bit(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        initBinaryLengthLengthTest(binaryLengthLength, binaryWireCode);
        BytesOut<?> bytes = Bytes.allocateDirect(65539);
        long pos = BinaryLengthLength.LENGTH_16BIT.initialise(bytes);
        bytes.writeSkip(65536);
        assertThrows(IllegalStateException.class, () -> BinaryLengthLength.LENGTH_16BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()));
        bytes.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    public void checkCodeMatches(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        initBinaryLengthLengthTest(binaryLengthLength, binaryWireCode);
        assertEquals(binaryWireCode, binaryLengthLength.code(), "binaryLengthLength.code");
    }

    @MethodSource("data")
    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    public void checkCodeIsWritten(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        initBinaryLengthLengthTest(binaryLengthLength, binaryWireCode);
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128);
        binaryLengthLength.initialise(bytes);
        byte readCode = (byte) bytes.readUnsignedByte();
        assertEquals((byte) binaryWireCode, readCode, "checkCodeIsWritten: assertEquals");
        bytes.releaseLast();
    }
}
