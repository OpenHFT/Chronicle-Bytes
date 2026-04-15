/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.Allocator.HEAP;
import static net.openhft.chronicle.bytes.Allocator.NATIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class Bytes2Test extends BytesTestCommon {

    private Allocator alloc1;
    private Allocator alloc2;

    public void initBytes2Test(Allocator alloc1, Allocator alloc2) {
        this.alloc1 = alloc1;
        this.alloc2 = alloc2;
    }

    public static Collection<Object[]> data() {
        if (Jvm.maxDirectMemory() == 0)
            return Arrays.asList(new Object[][]{{HEAP, HEAP}});
        return Arrays.asList(new Object[][]{
                {NATIVE, NATIVE}, {HEAP, NATIVE}, {NATIVE, HEAP}, {HEAP, HEAP}
        });
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testPartialWrite(Allocator alloc1, Allocator alloc2) {
        initBytes2Test(alloc1, alloc2);
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

    @MethodSource("data")
    @ParameterizedTest
    public void testPartialWrite64plus(Allocator alloc1, Allocator alloc2) {
        initBytes2Test(alloc1, alloc2);
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

    @MethodSource("data")
    @ParameterizedTest
    public void testWrite64plus(Allocator alloc1, Allocator alloc2) {
        initBytes2Test(alloc1, alloc2);
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

    @MethodSource("data")
    @ParameterizedTest
    public void testParseToBytes(Allocator alloc1, Allocator alloc2)
            throws IORuntimeException {
        initBytes2Test(alloc1, alloc2);
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
