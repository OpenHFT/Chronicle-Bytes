/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class BinaryLengthLengthTest extends BytesTestCommon {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(BinaryLengthLength.LENGTH_8BIT, BinaryWireCode.BYTES_LENGTH8),
                Arguments.of(BinaryLengthLength.LENGTH_16BIT, BinaryWireCode.BYTES_LENGTH16),
                Arguments.of(BinaryLengthLength.LENGTH_32BIT, BinaryWireCode.BYTES_LENGTH32)
        );
    }

    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    @MethodSource("data")
    void testInvalidLengthFor8Bit(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        BytesOut<?> bytes = Bytes.allocateDirect(512);
        long pos = BinaryLengthLength.LENGTH_8BIT.initialise(bytes);
        bytes.writeSkip(256);
        assertThrows(IllegalStateException.class, () -> BinaryLengthLength.LENGTH_8BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()));
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    @MethodSource("data")
    void testInvalidLengthFor16Bit(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        BytesOut<?> bytes = Bytes.allocateDirect(65539);
        long pos = BinaryLengthLength.LENGTH_16BIT.initialise(bytes);
        bytes.writeSkip(65536);
        assertThrows(IllegalStateException.class, () -> BinaryLengthLength.LENGTH_16BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()));
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    @MethodSource("data")
    void checkCodeMatches(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        assertEquals(binaryWireCode, binaryLengthLength.code());
    }

    @ParameterizedTest(name = "binaryLengthLength {0} binaryWireCode {1}")
    @MethodSource("data")
    void checkCodeIsWritten(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128);
        binaryLengthLength.initialise(bytes);
        byte readCode = (byte) bytes.readUnsignedByte();
        assertEquals((byte) binaryWireCode, readCode);
        bytes.releaseLast();
    }
}
