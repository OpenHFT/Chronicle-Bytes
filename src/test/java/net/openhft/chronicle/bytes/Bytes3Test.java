/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests various Bytes implementations across multiple factory methods because correct
 * behaviour must be verified for heap, direct, elastic, mapped, and checked variants
 * to avoid runtime failures in different deployment scenarios.
 */
@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
@DisplayName("Bytes - parameterised tests for read, write, append, and toString across allocators")
public class Bytes3Test extends BytesTestCommon {

    private static final String TMP_FILE = OS.getTarget() + "/Bytes3Test-deleteme";

    static Stream<Arguments> data() {
        List<Object[]> tests = new ArrayList<>(Arrays.asList(new Object[][]{
                {"Bytes::elasticHeapByteBuffer", (Supplier<Bytes<?>>) Bytes::elasticHeapByteBuffer},
                {"Bytes.elasticHeapByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260)},
                {"Bytes.elasticHeapByteBuffer(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260).unchecked(true)},
                {"Bytes::allocateElasticOnHeap", (Supplier<Bytes<?>>) Bytes::allocateElasticOnHeap},
                {"Bytes.wrapForRead(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(new byte[1024])},
                {"Bytes.wrapForWrite(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(new byte[1024])},
                {"new HexDumpBytes()", (Supplier<Bytes<?>>) HexDumpBytes::new},
        }));
        if (Jvm.maxDirectMemory() > 0) {
            tests.addAll(Arrays.asList(new Object[][]{
                    {"Bytes.elasticByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260)},
                    {"Bytes.elasticByteBuffer(260, 1025)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260, 1025)},
                    {"Bytes.allocateElasticDirect(260)", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260)},
                    {"Bytes.allocateElasticDirect(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260).unchecked(true)},
                    {"Bytes.wrapForRead(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(ByteBuffer.allocateDirect(260))},
                    {"Bytes.wrapForWrite(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(ByteBuffer.allocateDirect(260))},
                    {"MappedBytes.mappedBytes(64K)", (Supplier<Bytes<?>>) () -> {
                        try {
                            // MappedBytes requires file-backed storage for memory-mapped IO
                            return MappedBytes.mappedBytes(TMP_FILE, 64 << 10);
                        } catch (FileNotFoundException e) {
                            // Re-throw wrapped for test infrastructure
                            throw Jvm.rethrow(e);
                        }
                    }}
            }));
        }
        new File(TMP_FILE).deleteOnExit();
        return tests.stream()
                .map(args -> Arguments.of(args[0], args[1]));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("readPosition starts at zero for new bytes instance")
    public void readPositionAt0(String name, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(0L, bytes.readPosition(),
                    "readPosition starts at zero for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("writePosition starts at zero for writable bytes instance")
    public void writePositionAt0(String name, Supplier<Bytes<?>> supplier) {
        boolean forRead = name.contains("ForRead");
        if (forRead) {
            return;
        }
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(0L, bytes.writePosition(),
                    "writePosition starts at zero for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("isClear reports true for new bytes buffer state")
    public void isClear(String name, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertTrue(bytes.isClear(),
                    "buffer reports clear state for new " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("byteOrder matches native order for bytes")
    public void byteOrder(String name, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder(),
                    "byteOrder matches native order for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("writeLimit is at least initial capacity limit")
    public void writeLimit(String name, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertTrue(bytes.writeLimit() >= 260,
                    "capacity limit " + bytes.writeLimit() + " is at least 260 for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write and clear cycle updates buffer state flags")
    public void write(String name, Supplier<Bytes<?>> supplier) {
        boolean forRead = name.contains("ForRead");
        if (forRead) {
            return;
        }
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(0, bytes.writePosition(),
                    "writePosition starts at zero before write for " + name);
            assertTrue(bytes.isClear(),
                    "buffer reports clear state before write for " + name);
            bytes.writeInt(42);
            assertEquals(42, bytes.readInt(),
                    "readInt returns written value for " + name);
            assertFalse(bytes.isClear(),
                    "buffer reports non-clear state after write for " + name);
            bytes.clear();
            assertTrue(bytes.isClear(),
                    "isClear returns true after clear for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    private void doAppend(String name,
                          Supplier<Bytes<?>> supplier,
                          boolean forRead,
                          BiConsumer<Bytes, CharSequence> append) {
        if (forRead) {
            return;
        }
        Bytes<?> bytes = supplier.get();
        try {
            append.accept(bytes, "Hello World".substring(1, 6));
            // binary format
            if (bytes.peekUnsignedByte() == 5)
                bytes.readSkip(1);
            assertEquals("ello ", bytes.toString(),
                    "append writes expected substring for " + name);
            bytes.clear();
            append.accept(bytes, "Oh, Hello World".split(" ")[1]);
            // binary format
            if (bytes.peekUnsignedByte() == 5)
                bytes.readSkip(1);
            assertEquals("Hello", bytes.toString(),
                    "append writes expected token for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append substring using ByteStringAppender helper method")
    public void appendSubstring(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), ByteStringAppender::append);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append bounded bytes from Bytes instance slice")
    public void appendBytesBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.append(Bytes.from("[" + s + "]"), 1, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append bounded substring from String value")
    public void appendStringBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.append("[" + s + "]", 1, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append 8bit substring using ByteStringAppender helper")
    public void append8bitSubstring(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), ByteStringAppender::append8bit);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append 8bit string value into bytes")
    public void append8bitString(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.append8bit(s.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append 8bit from Bytes wrapper instance")
    public void append8bitFromBytes(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.append8bit(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append 8bit from bounded Bytes wrapper")
    public void append8bitFromBytesBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.append8bit(Bytes.from("[" + s + "]"), 1L, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append 8bit bounded string value slice")
    public void append8bitStringBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.append8bit("[" + s + "]", 1, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write substring using ByteStringAppender helper method")
    public void writeSubstring(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), ByteStringAppender::write);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write bytes from Bytes instance value buffer source")
    public void writeFromBytes(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write from byte array using US-ASCII encoding")
    public void writeByteArray(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write(s.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write bounded bytes from Bytes instance slice")
    public void writeFromBytesBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write(Bytes.from("[" + s + "]"), 1L, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write CharSequence from Bytes view slice")
    public void writeFromBytes2(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write((CharSequence) Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("append UTF8 substring using ByteStringAppender helper")
    public void appendUtf8Substring(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), ByteStringAppender::appendUtf8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write 8bit substring using ByteStringAppender helper")
    public void write8bitSubstring(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), ByteStringAppender::write8bit);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write 8bit substring from String value")
    public void write8bitSubstring2(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write8bit(s.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write 8bit substring bounded range slice")
    public void write8bitSubstringBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write8bit(s, 0, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write 8bit from Bytes wrapper instance")
    public void write8bitFromBytes(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write8bit(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write 8bit bounded Bytes wrapper instance")
    public void write8bitFromBytesBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write8bit(Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write 8bit bounded string value slice")
    public void write8bitStringBounded(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.write8bit("[" + s + "]", 1, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write UTF8 substring using ByteStringAppender helper")
    public void writeUtf8Substring(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), ByteStringAppender::writeUtf8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write UTF8 substring from String value")
    public void writeUtf8Substring2(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.writeUtf8(s.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write UTF8 from Bytes wrapper instance")
    public void writeUtf8FromBytes(String name, Supplier<Bytes<?>> supplier) {
        doAppend(name, supplier, name.contains("ForRead"), (b, s) -> b.writeUtf8(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @SuppressWarnings("rawtypes")
    @DisplayName("toString8bit produces expected character mapping output")
    public void toString8bit(String name, Supplier<Bytes<?>> supplier) {
        boolean forRead = name.contains("ForRead");
        if (forRead) {
            return;
        }
        Bytes<?> bytes = supplier.get();
        try {
            for (char ch = 0; ch < 256; ch++) {
                bytes.writeUnsignedByte(ch);
            }
            String s = bytes.toString();
            for (char ch = 0; ch < 256; ch++) {
                assertEquals(ch, s.charAt(ch),
                        "Character mismatch at index " + (int) ch + " in: " + s);
            }
            assertEquals(256, s.length(),
                    "toString should return 256 characters, but got: " + s.length());
        } finally {
            bytes.releaseLast();
        }
    }
}
