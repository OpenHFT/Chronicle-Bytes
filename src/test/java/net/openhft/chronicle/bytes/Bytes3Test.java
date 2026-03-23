/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
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

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Bytes3Test extends BytesTestCommon {

    private static final String TMP_FILE = OS.getTarget() + "/Bytes3Test-deleteme";

    static Stream<Arguments> data() {
        List<Arguments> tests = new ArrayList<>(Arrays.asList(
                Arguments.of("Bytes::elasticHeapByteBuffer", (Supplier<Bytes<?>>) Bytes::elasticHeapByteBuffer),
                Arguments.of("Bytes.elasticHeapByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260)),
                Arguments.of("Bytes.elasticHeapByteBuffer(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260).unchecked(true)),
                Arguments.of("Bytes::allocateElasticOnHeap", (Supplier<Bytes<?>>) Bytes::allocateElasticOnHeap),
                Arguments.of("Bytes.wrapForRead(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(new byte[1024])),
                Arguments.of("Bytes.wrapForWrite(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(new byte[1024])),
                Arguments.of("new HexDumpBytes()", (Supplier<Bytes<?>>) HexDumpBytes::new)
        ));
        if (Jvm.maxDirectMemory() > 0) {
            tests.addAll(Arrays.asList(
                    Arguments.of("Bytes.elasticByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260)),
                    Arguments.of("Bytes.elasticByteBuffer(260, 1025)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260, 1025)),
                    Arguments.of("Bytes.allocateElasticDirect(260)", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260)),
                    Arguments.of("Bytes.allocateElasticDirect(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260).unchecked(true)),
                    Arguments.of("Bytes.wrapForRead(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(ByteBuffer.allocateDirect(260))),
                    Arguments.of("Bytes.wrapForWrite(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(ByteBuffer.allocateDirect(260))),
                    Arguments.of("MappedBytes.mappedBytes(64K)", (Supplier<Bytes<?>>) () -> {
                        try {
                            return MappedBytes.mappedBytes(TMP_FILE, 64 << 10);
                        } catch (FileNotFoundException e) {
                            throw Jvm.rethrow(e);
                        }
                    })
            ));
        }
        return tests.stream();
    }

    private void releaseBytes(Bytes<?> bytes) {
        if (bytes instanceof MappedBytes)
            ((MappedBytes) bytes).close();
        else if (bytes != null)
            bytes.releaseLast();
        new File(TMP_FILE).deleteOnExit();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void readPositionAt0(String testName, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(0L, bytes.readPosition());
        } finally {
            releaseBytes(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writePositionAt0(String testName, Supplier<Bytes<?>> supplier) {
        boolean forRead = testName.contains("ForRead");
        if (forRead) return;
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(0L, bytes.writePosition());
        } finally {
            releaseBytes(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void isClear(String testName, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertTrue(bytes.isClear());
        } finally {
            releaseBytes(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void byteOrder(String testName, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertSame(ByteOrder.nativeOrder(), bytes.byteOrder());
        } finally {
            releaseBytes(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeLimit(String testName, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            assertTrue(bytes.writeLimit() >= 260);
        } finally {
            releaseBytes(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write(String testName, Supplier<Bytes<?>> supplier) {
        boolean forRead = testName.contains("ForRead");
        if (forRead) return;
        Bytes<?> bytes = supplier.get();
        try {
            assertEquals(0, bytes.writePosition());
            assertTrue(bytes.isClear());
            bytes.writeInt(42);
            assertEquals(42, bytes.readInt());
            assertFalse(bytes.isClear());
            bytes.clear();
            assertTrue(bytes.isClear());
        } finally {
            releaseBytes(bytes);
        }
    }

    private void doAppend(String testName, Supplier<Bytes<?>> supplier, BiConsumer<Bytes, CharSequence> append) {
        boolean forRead = testName.contains("ForRead");
        if (forRead) return;
        Bytes<?> bytes = supplier.get();
        try {
            append.accept(bytes, "Hello World".substring(1, 6));
            // binary format
            if (bytes.peekUnsignedByte() == 5)
                bytes.readSkip(1);
            assertEquals("ello ", bytes.toString());
            bytes.clear();
            append.accept(bytes, "Oh, Hello World".split(" ")[1]);
            // binary format
            if (bytes.peekUnsignedByte() == 5)
                bytes.readSkip(1);
            assertEquals("Hello", bytes.toString());
        } finally {
            releaseBytes(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void appendSubstring(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, ByteStringAppender::append);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void appendBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.append(Bytes.from("[" + s + "]"), 1, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void appendStringBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.append("[" + s + "]", 1, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void append8bitSubstring(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, ByteStringAppender::append8bit);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void append8bitString(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.append8bit(s.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void append8bitFromBytes(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.append8bit(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void append8bitFromBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.append8bit(Bytes.from("[" + s + "]"), 1L, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void append8bitStringBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.append8bit("[" + s + "]", 1, s.length() + 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeSubstring(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, ByteStringAppender::write);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeFromBytes(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeByteArray(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write(s.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeFromBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write(Bytes.from("[" + s + "]"), 1L, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeFromBytes2(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write((CharSequence) Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void appendUtf8Substring(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, ByteStringAppender::appendUtf8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write8bitSubstring(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, ByteStringAppender::write8bit);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write8bitSubstring2(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write8bit(s.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write8bitSubstringBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write8bit(s, 0, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write8bitFromBytes(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write8bit(Bytes.from(s)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write8bitFromBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write8bit(Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void write8bitStringBounded(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.write8bit("[" + s + "]", 1, s.length()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeUtf8Substring(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, ByteStringAppender::writeUtf8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeUtf8Substring2(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.writeUtf8(s.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void writeUtf8FromBytes(String testName, Supplier<Bytes<?>> supplier) {
        doAppend(testName, supplier, (b, s) -> b.writeUtf8(Bytes.from(s)));
    }

    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void toString8bit(String testName, Supplier<Bytes<?>> supplier) {
        boolean forRead = testName.contains("ForRead");
        if (forRead) return;
        Bytes<?> bytes = supplier.get();
        try {
            for (char ch = 0; ch < 256; ch++) {
                bytes.writeUnsignedByte(ch);
            }
            String s = bytes.toString();
            for (char ch = 0; ch < 256; ch++) {
                assertEquals(ch, s.charAt(ch), "Character mismatch at index " + ch + " in: " + s);
            }
            assertEquals(256, s.length(), "Expected 256 characters, but got: " + s.length());
        } finally {
            releaseBytes(bytes);
        }
    }
}
