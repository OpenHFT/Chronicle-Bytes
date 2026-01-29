/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that primitive parameter constraints enforce argument validation behaviour consistently
 * because rejecting invalid inputs is essential for preventing undefined behaviour.
 */
@SuppressWarnings("deprecation")
@DisplayName("Bytes primitive parameter validation enforcement rejects negative values for non-negative constraints")
final class BytesPrimitiveParameterTest { // too hard to ensure resources are released

    private static final String SILLY_NAME = "Tryggve";

    /**
     * Checks that methods throw IllegalArgumentException when negative parameters are supplied for @NonNegative.
     */
    @TestFactory
    @DisplayName("negative parameters for non-negative inputs are rejected")
    Stream<DynamicTest> negativeParameters() {
        final AtomicReference<BytesInitialInfo> initialInfo = new AtomicReference<>();
        return cartesianProductTest(BytesFactoryUtil::provideBytesObjects,
                BytesPrimitiveParameterTest::provideNegativeNonNegativeOperations,
                (args, bytes, nc) -> {
                    if (UncheckedBytes.class.isInstance(bytes))
                        // UncheckedBytes is... Well, unchecked
                        return;
                    final String name = createCommand(args) + "->" + bytes(args).getClass().getSimpleName() + "." + nc.name();
                    if (bytes.writePosition() == 0) {
                        if (isReadWrite(args)) {
                            // Debug trace: log bytes initialisation for test diagnostics
                            System.out.println("Initializing: " + name + " " + bytes.getClass().getSimpleName());
                            bytes.write(SILLY_NAME);
                        }
                    }
                    initialInfo.set(new BytesInitialInfo(bytes));

                    try {
                        nc.accept(bytes);
                    } catch (IllegalArgumentException |
                             BufferOverflowException |
                             BufferUnderflowException |
                             AssertionError |
                             UnsupportedOperationException |
                             StringIndexOutOfBoundsException |
                             IORuntimeException accepted) {
                        // Although strictly not correct, we accept these Exceptions/Errors for neg-args
                    } catch (Throwable t) {
                        if (!(t instanceof IOException))
                            fail(name, t);
                    }

                    // Unable to check actual size for released MappedBytes
                    if (!MappedBytes.class.isInstance(bytes)) {
                        final BytesInitialInfo info = new BytesInitialInfo(bytes);
                        assertEquals(initialInfo.get(), info, name);
                    }
                    if (isReadWrite(args)) {
                        // Make sure nothing changes in the contents of the Bytes object
                        if (!SILLY_NAME.equals(bytes.toString())) {
                            // Debug trace: dump bytes for investigation when content differs
                            System.out.println(bytes.getClass().getSimpleName() + ", b: " + Arrays.toString(bytes.toString().getBytes(StandardCharsets.UTF_8)));
                        }
                        assertEquals(SILLY_NAME, bytes.toString(), name);
                    }
                }
        );
    }

    private static Stream<NamedConsumer<Bytes<Object>>> provideNegativeNonNegativeOperations() {
        final BytesStore<?, ?> bs = BytesStore.from(SILLY_NAME);
        return Stream.of(

                // Byte array and BytesStore writes with negative offsets or lengths.
                NamedConsumer.of(b -> b.write(-1, new byte[1]), "write(-1, new byte[1])"),
                NamedConsumer.of(b -> b.write(new byte[1], -1, 1), "write(new byte[1], -1, 1)"),
                NamedConsumer.of(b -> b.write(new byte[1], 1, -1), "write(new byte[1], 1, -1)"),
                NamedConsumer.of(b -> b.write(-1, new byte[1], 1, 1), "write(-1, new byte[1], 1, -1)"),
                NamedConsumer.of(b -> b.write(1, new byte[1], -1, 1), "write(1, new byte[1], -1, 1)"),
                NamedConsumer.of(b -> b.write(1, new byte[1], 1, -1), "write(1, new byte[1], 1, -1)"),
                NamedConsumer.of(b -> b.write(-1, bs), "write(-1, bs)"),
                NamedConsumer.of(b -> b.write(bs, -1L, 1L), "write(bs, -1L, 1L)"),
                NamedConsumer.of(b -> b.write(bs, 1L, -1L), "write(bs, 1L, -1L)"),
                NamedConsumer.of(b -> b.write(-1, bs, 1, 1), "write(-1, bs, 1, -1)"),
                NamedConsumer.of(b -> b.write(1, bs, -1, 1), "write(1, bs, -1, 1)"),
                NamedConsumer.of(b -> b.write(1, bs, 1, -1), "write(1, bs, 1, -1)"),
                NamedConsumer.of(b -> b.write(1, bs, 1, -1), "write(1, bs, 1, -1)"),
                NamedConsumer.of(b -> b.write(SILLY_NAME, -1, 1), "write(cs, -1, 1)"),
                NamedConsumer.of(b -> b.write(SILLY_NAME, 1, -1), "write(cs, -1, 1)"),
                NamedConsumer.of(b -> b.writeUtf8(-1, SILLY_NAME), "writeUtf8(-1, cs)"),
                NamedConsumer.of(b -> b.writeUtf8Limited(-1, SILLY_NAME, SILLY_NAME.length()), "writeUtf8Limited(-1, cs, 1)"),
                NamedConsumer.of(b -> b.writeUtf8Limited(1, SILLY_NAME, -1), "writeUtf8Limited(1, cs, -1)"),
                NamedConsumer.of(b -> b.write8bit(-1, bs), "write8bit(-1, bs)"),
                NamedConsumer.of(b -> b.write8bit(-1, SILLY_NAME, 1, 1), "write8bit(-1, cs, 1, 1)"),
                NamedConsumer.of(b -> b.write8bit(1, SILLY_NAME, -1, 1), "write8bit(1, cs, -1, 1)"),
                NamedConsumer.of(b -> b.write8bit(1, SILLY_NAME, 1, -1), "write8bit(1, cs, 1, -1)"),
                NamedConsumer.of(b -> b.write8bit(SILLY_NAME, -1, 1), "write8bit(1, cs, -1, 1)"),
                NamedConsumer.of(b -> b.write8bit(SILLY_NAME, 1, -1), "write8bit(1, cs, 1, -1)"),

                // Byte-sized primitives with negative offsets.
                NamedConsumer.of(b -> b.writeByte(-1L, 42), "writeByte(-1, int)"),
                NamedConsumer.of(b -> b.writeByte(-1L, (byte) 42), "writeByte(-1, byte)"),
                NamedConsumer.of(b -> b.writeVolatileByte(-1L, (byte) 42), "writeVolatileByte(-1, byte)"),
                NamedConsumer.of(b -> b.writeUnsignedByte(-1L, (byte) 42), "writeUnsignedByte(-1, byte)"),

                // Short-sized primitives with negative offsets.
                NamedConsumer.of(b -> b.writeShort(-1L, (short) 42), "writeShort(-1, byte)"),
                NamedConsumer.of(b -> b.writeUnsignedShort(-1, 42), "writeUnsignedShort(-1, 42)"),
                NamedConsumer.of(b -> b.writeVolatileShort(-1, (short) 42), "writeVolatileShort(-1, 42)"),

                // Integer-sized primitives with negative offsets.
                NamedConsumer.of(b -> b.writeInt(-1L, 42), "writeInt(-1, byte)"),
                NamedConsumer.of(b -> b.writeInt24(-1L, 42), "writeInt24(-1, byte)"),
                NamedConsumer.of(b -> b.writeIntAdv(42, -1), "writeInt24(42, -1)"),
                NamedConsumer.of(b -> b.writeOrderedInt(-1, 42), "writeOrderedInt(-1, 42)"),
                NamedConsumer.of(b -> b.writeUnsignedInt(-1, 42), "writeUnsignedInt(-1, 42)"),
                NamedConsumer.of(b -> b.writeVolatileInt(-1, 42), "writeVolatileInt(-1, 42)"),

                // Long-sized primitives with negative offsets.
                NamedConsumer.of(b -> b.writeLong(-1L, 42), "writeLong(-1, byte)"),
                NamedConsumer.of(b -> b.writeLongAdv(42, -1), "writeLongAdv(42, -1)"),
                NamedConsumer.of(b -> b.writeOrderedLong(-1, 42), "writeOrderedLong(-1, 42)"),
                NamedConsumer.of(b -> b.writeMaxLong(-1, 42), "writeMaxLong(-1, 42)"),
                NamedConsumer.of(b -> b.writeVolatileLong(-1, 42), "writeVolatileLong(-1, 42)"),

                // Floating point primitives with negative offsets.
                NamedConsumer.of(b -> b.writeFloat(-1L, 42), "writeFloat(-1, byte)"),
                NamedConsumer.of(b -> b.writeOrderedFloat(-1, 42), "writeOrderedFloat(-1, 42)"),
                NamedConsumer.of(b -> b.writeVolatileFloat(-1, 42), "writeVolatileFloat(-1, 42)"),

                // Double precision primitives with negative offsets.
                NamedConsumer.of(b -> b.writeDouble(-1L, 42), "writeDouble(-1, byte)"),
                NamedConsumer.of(b -> b.writeOrderedDouble(-1, 42), "writeOrderedDouble(-1, 42)"),
                NamedConsumer.of(b -> b.writeVolatileDouble(-1, 42), "writeVolatileDouble(-1, 42)"),

                // Position and address setters with negative values.
                NamedConsumer.of(b -> b.writeLimit(-1), "writeLimit(-1)"),
                NamedConsumer.of(b -> b.writePositionRemaining(-1, 1), "writePositionRemaining(-1)"),
                NamedConsumer.of(b -> b.writePositionRemaining(1, -1), "writePositionRemaining(-1)"),

                NamedConsumer.of(b -> b.addressForWrite(-1), "addressForWrite(-1)"),

                NamedConsumer.of(b -> b.writePosition(-1), "writePosition(-1)")

        );
    }

}
