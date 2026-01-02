/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.Allocator.HEAP;
import static net.openhft.chronicle.bytes.Allocator.NATIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class Bytes2Test extends BytesTestCommon {

    static Stream<Arguments> data() {
        if (Jvm.maxDirectMemory() == 0)
            return Stream.of(Arguments.of(HEAP, HEAP));
        return Stream.of(
                Arguments.of(NATIVE, NATIVE),
                Arguments.of(HEAP, NATIVE),
                Arguments.of(NATIVE, HEAP),
                Arguments.of(HEAP, HEAP)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("partial write copies expected prefix bytes")
    public void testPartialWrite(Allocator alloc1, Allocator alloc2) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for partial prefix writes");

        Bytes<?> from = alloc1.elasticBytes(1);
        Bytes<?> to = alloc2.fixedBytes(6);

        try {
            from.write("Hello World");

            ByteBuffer buffer = from.toTemporaryDirectByteBuffer();
            to.writeSome(buffer);
            assertEquals("Hello ", to.toString(),
                    "Partial write copies expected prefix into target");
            assertEquals("Hello World", from.toString(),
                    "Source content remains unchanged after partial write");
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("partial write handles payloads larger than 64 bytes")
    public void testPartialWrite64plus(Allocator alloc1, Allocator alloc2) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for 64+ partial writes");
        Bytes<?> from = alloc1.elasticBytes(1);
        Bytes<?> to = alloc2.fixedBytes(6);

        from.write("Hello World 0123456789012345678901234567890123456789012345678901234567890123456789");

        try {
            to.writeSome(from.toTemporaryDirectByteBuffer());
            String fromValue = from.toString();
            assertTrue(fromValue.startsWith("Hello World "),
                    "Source value " + fromValue + " retains expected prefix");
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("write copies full payload for 64+ byte strings")
    public void testWrite64plus(Allocator alloc1, Allocator alloc2) {
        Bytes<?> from = alloc1.fixedBytes(128);
        Bytes<?> to = alloc2.fixedBytes(128);

        from.write("Hello World 0123456789012345678901234567890123456789012345678901234567890123456789");

        try {
            to.write(from);
            assertEquals(from.toString(), to.toString(),
                    "write copies full payload between Bytes instances");
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("parse8bit copies tokens into target bytes")
    public void testParseToBytes(Allocator alloc1, Allocator alloc2)
            throws IORuntimeException {
        Bytes<?> from = alloc1.fixedBytes(64);
        Bytes<?> to = alloc2.fixedBytes(32);
        try {
            from.append8bit("0123456789 aaaaaaaaaa 0123456789 0123456789");

            for (int i = 0; i < 4; i++) {
                from.parse8bit(to, StopCharTesters.SPACE_STOP);
                assertEquals(10, to.readRemaining(),
                        "Parsed token length remains 10 at index " + i);
            }
            assertEquals(0, from.readRemaining(),
                    "Source is fully consumed after parsing tokens");
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }
}
