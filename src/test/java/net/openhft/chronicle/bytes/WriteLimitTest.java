/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class WriteLimitTest extends BytesTestCommon {
    private static final Allocator[] ALLOCATORS = {Allocator.HEAP, Allocator.HEAP_EMBEDDED, Allocator.HEAP_UNCHECKED};
    private static List<Object[]> tests;
    static Random random = new Random();
    private final String name;
    private final Allocator allocator;
    private final Consumer<Bytes<?>> action;
    private final int length;

    public WriteLimitTest(String name, Allocator allocator, Consumer<Bytes<?>> action, int length) {
        this.name = name;
        this.allocator = allocator;
        this.action = action;
        this.length = length;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        tests = new ArrayList<>();
        addTest("boolean", b -> b.writeBoolean(true), 1);
        addTest("byte", b -> b.writeByte((byte) 1), 1);
        addTest("unsigned-byte", b -> b.writeUnsignedByte(1), 1);
        addTest("short", b -> b.writeShort((short) 1), 2);
        addTest("unsigned-short", b -> b.writeUnsignedShort(1), 2);
        addTest("char $", b -> b.writeChar('$'), 1);
        addTest("char £", b -> b.writeChar('£'), 2);
        addTest("char " + (char) (1 << 14), b -> b.writeChar((char) (1 << 14)), 3);
        addTest("int", b -> b.writeInt(1), 4);
        addTest("unsigned-int", b -> b.writeUnsignedInt(1), 4);
        addTest("float", b -> b.writeFloat(1), 4);
        addTest("long", b -> b.writeLong(1), 8);
        addTest("double", b -> b.writeDouble(1), 8);
        return tests;
    }

    private static void addTest(String name, Consumer<Bytes<?>> action, int length) {
        Allocator[] allocators = Jvm.maxDirectMemory() == 0 ? ALLOCATORS : Allocator.values();
        for (Allocator a : allocators)
            tests.add(new Object[]{a + " " + name, a, action, length});
    }

    @SuppressWarnings("RedundantCast")
    @Test
    public void writeLimit() {
        Bytes<?> bytes = allocator.elasticBytes(64);
        for (int i = 0; i < 16; i++) {
            int position = (int) (bytes.realCapacity() - length - i);
            bytes.clear().writePosition(position).writeLimit(position + length);
            action.accept(bytes);
            if (bytes.unchecked())
                continue;

            bytes.clear().writePosition(position).writeLimit(position + length - 1);
            try {
                action.accept(bytes);
                fail("position: " + position);
            } catch (BufferOverflowException ignored) {
                // expected
            }
        }
        bytes.releaseLast();
    }
}
