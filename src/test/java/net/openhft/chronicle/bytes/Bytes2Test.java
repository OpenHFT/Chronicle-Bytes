/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.Allocator.HEAP;
import static net.openhft.chronicle.bytes.Allocator.NATIVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class Bytes2Test extends BytesTestCommon {

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
    void testPartialWrite(Allocator alloc1, Allocator alloc2) {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> from = alloc1.elasticBytes(1);
        Bytes<?> to = alloc2.fixedBytes(6);

        try {
            from.write("Hello World");

            ByteBuffer buffer = from.toTemporaryDirectByteBuffer();
            to.writeSome(buffer);
            assertEquals("Hello ", to.toString());
            assertEquals("Hello World", from.toString());
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void testPartialWrite64plus(Allocator alloc1, Allocator alloc2) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> from = alloc1.elasticBytes(1);
        Bytes<?> to = alloc2.fixedBytes(6);

        from.write("Hello World 0123456789012345678901234567890123456789012345678901234567890123456789");

        try {
            to.writeSome(from.toTemporaryDirectByteBuffer());
            assertTrue(from.toString().startsWith("Hello World "), "from: " + from);
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void testWrite64plus(Allocator alloc1, Allocator alloc2) {
        Bytes<?> from = alloc1.fixedBytes(128);
        Bytes<?> to = alloc2.fixedBytes(128);

        from.write("Hello World 0123456789012345678901234567890123456789012345678901234567890123456789");

        try {
            to.write(from);
            assertEquals(from.toString(), to.toString());
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void testParseToBytes(Allocator alloc1, Allocator alloc2)
            throws IORuntimeException {
        Bytes<?> from = alloc1.fixedBytes(64);
        Bytes<?> to = alloc2.fixedBytes(32);
        try {
            from.append8bit("0123456789 aaaaaaaaaa 0123456789 0123456789");

            for (int i = 0; i < 4; i++) {
                from.parse8bit(to, StopCharTesters.SPACE_STOP);
                assertEquals(10, to.readRemaining());
            }
            assertEquals(0, from.readRemaining());
        } finally {
            from.releaseLast();
            to.releaseLast();
        }
    }
}
