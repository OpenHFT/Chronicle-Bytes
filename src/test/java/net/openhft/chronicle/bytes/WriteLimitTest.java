/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Parameterised tests checking that write limits and elastic resizing behave
 * correctly across different allocator strategies because correct boundary
 * enforcement is essential to avoid silent data corruption.
 */
@DisplayName("WriteLimit - validates write limit enforcement across allocators")
public class WriteLimitTest extends BytesTestCommon {
    private static final Allocator[] ALLOCATORS = {Allocator.HEAP, Allocator.HEAP_EMBEDDED, Allocator.HEAP_UNCHECKED};

    public static Stream<Arguments> data() {
        List<Arguments> tests = new ArrayList<>();
        addTest(tests, "boolean", b -> b.writeBoolean(true), 1);
        addTest(tests, "byte", b -> b.writeByte((byte) 1), 1);
        addTest(tests, "unsigned-byte", b -> b.writeUnsignedByte(1), 1);
        addTest(tests, "short", b -> b.writeShort((short) 1), 2);
        addTest(tests, "unsigned-short", b -> b.writeUnsignedShort(1), 2);
        addTest(tests, "char $", b -> b.writeChar('$'), 1);
        addTest(tests, "char £", b -> b.writeChar('£'), 2);
        addTest(tests, "char " + (char) (1 << 14), b -> b.writeChar((char) (1 << 14)), 3);
        addTest(tests, "int", b -> b.writeInt(1), 4);
        addTest(tests, "unsigned-int", b -> b.writeUnsignedInt(1), 4);
        addTest(tests, "float", b -> b.writeFloat(1), 4);
        addTest(tests, "long", b -> b.writeLong(1), 8);
        addTest(tests, "double", b -> b.writeDouble(1), 8);
        return tests.stream();
    }

    private static void addTest(List<Arguments> tests, String name, Consumer<Bytes<?>> action, int length) {
        Allocator[] allocators = Jvm.maxDirectMemory() == 0 ? ALLOCATORS : Allocator.values();
        for (Allocator a : allocators)
            tests.add(Arguments.of(a + " " + name, a, action, length));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @DisplayName("write limit rejects overflow writes for allocator scenarios")
    @MethodSource("data")
    public void writeLimit(String name, Allocator allocator, Consumer<Bytes<?>> action, int length) {
        Bytes<?> bytes = allocator.elasticBytes(64);
        try {
            for (int i = 0; i < 16; i++) {
                int position = (int) (bytes.realCapacity() - length - i);
                bytes.clear().writePosition(position).writeLimit(position + length);
                action.accept(bytes);
                if (bytes.unchecked())
                    continue;

                bytes.clear().writePosition(position).writeLimit(position + length - 1);
                assertThrows(BufferOverflowException.class,
                        () -> action.accept(bytes),
                        "Overflow expected at index " + i + " for " + name
                                + " with position " + position + " and length " + length);
            }
        } finally {
            bytes.releaseLast();
        }
    }
}
