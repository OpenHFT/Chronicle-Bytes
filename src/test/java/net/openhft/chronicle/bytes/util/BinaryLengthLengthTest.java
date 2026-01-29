/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BinaryWireCode;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests binary length encoding because correct bit-width selection is
 * essential to avoid truncation when serialising large payloads.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("BinaryLengthLength - validates 8/16/32-bit length wire codes")
public class BinaryLengthLengthTest extends BytesTestCommon {

    private static Stream<Arguments> lengthAndCode() {
        return Stream.of(
                Arguments.of(BinaryLengthLength.LENGTH_8BIT, BinaryWireCode.BYTES_LENGTH8),
                Arguments.of(BinaryLengthLength.LENGTH_16BIT, BinaryWireCode.BYTES_LENGTH16),
                Arguments.of(BinaryLengthLength.LENGTH_32BIT, BinaryWireCode.BYTES_LENGTH32)
        );
    }

    @BeforeEach
    void hasDirect() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for length tests");
    }

    @Test
    @DisplayName("8-bit length rejects values above 255")
    public void testInvalidLengthFor8Bit() {
        BytesOut<?> bytes = Bytes.allocateDirect(512);
        try {
            long pos = BinaryLengthLength.LENGTH_8BIT.initialise(bytes);
            bytes.writeSkip(256);
            assertThrows(IllegalStateException.class,
                    () -> BinaryLengthLength.LENGTH_8BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()),
                    "8-bit length should reject values above 255");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("16-bit length rejects values above 65535")
    public void testInvalidLengthFor16Bit() {
        BytesOut<?> bytes = Bytes.allocateDirect(65539);
        try {
            long pos = BinaryLengthLength.LENGTH_16BIT.initialise(bytes);
            bytes.writeSkip(65536);
            assertThrows(IllegalStateException.class,
                    () -> BinaryLengthLength.LENGTH_16BIT.writeLength((Bytes<?>) bytes, pos, bytes.writePosition()),
                    "16-bit length should reject values above 65535");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("32-bit length rejects values above 2^31-1")
    public void testInvalidLengthFor32Bit() {
        BytesOut<?> bytes = Bytes.allocateDirect(64);
        try {
            long pos = BinaryLengthLength.LENGTH_32BIT.initialise(bytes);
            long end = pos + (1L << 31) + 4;
            assertThrows(IllegalStateException.class,
                    () -> BinaryLengthLength.LENGTH_32BIT.writeLength((Bytes<?>) bytes, pos, end),
                    "32-bit length should reject values above 2^31-1");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("32-bit length writes the computed length value")
    public void testValidLengthFor32Bit() {
        BytesOut<?> bytes = Bytes.allocateDirect(64);
        try {
            long pos = BinaryLengthLength.LENGTH_32BIT.initialise(bytes);
            bytes.writeSkip(12);
            long end = bytes.writePosition();
            BinaryLengthLength.LENGTH_32BIT.writeLength((Bytes<?>) bytes, pos, end);
            assertEquals(12,
                    ((Bytes<?>) bytes).readInt(pos),
                    "32-bit length should store the computed length");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{index}: {0} uses wire code {1}")
    @MethodSource("lengthAndCode")
    @DisplayName("wire code matches the configured length format")
    public void checkCodeMatches(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        assertEquals(binaryWireCode,
                binaryLengthLength.code(),
                "Length " + binaryLengthLength + " should report the expected wire code");
    }

    @ParameterizedTest(name = "{index}: {0} writes code byte {1}")
    @MethodSource("lengthAndCode")
    @DisplayName("wire code is written to the output buffer")
    public void checkCodeIsWritten(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128);
        try {
            binaryLengthLength.initialise(bytes);
            byte readCode = (byte) bytes.readUnsignedByte();
            assertEquals((byte) binaryWireCode,
                    readCode,
                    "Initialise should write the expected wire code byte");
        } finally {
            bytes.releaseLast();
        }
    }
}
