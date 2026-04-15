/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Bytes3Test extends BytesTestCommon {

    private static final String TMP_FILE = OS.getTarget() + "/Bytes3Test-deleteme";
    private Supplier<Bytes<?>> supplier;
    private boolean forRead;
    private Bytes<?> bytes;

    public void initBytes3Test(String testName, Supplier<Bytes<?>> supplier) {
        this.supplier = supplier;
        this.forRead = testName.contains("ForRead");
    }

    public static Collection<Object[]> data() {
        List<Object[]> tests = new ArrayList<>(Arrays.asList(new Object[][]{
                {"Bytes::elasticHeapByteBuffer", (Supplier<Bytes<?>>) Bytes::elasticHeapByteBuffer},
                {"Bytes.elasticHeapByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260)},
                {"Bytes.elasticHeapByteBuffer(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260).unchecked(true)},
                {"Bytes::allocateElasticOnHeap", (Supplier<Bytes<?>>) Bytes::allocateElasticOnHeap},
                {"Bytes.wrapForRead(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(new byte[1024])},
                {"Bytes.wrapForWrite(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(new byte[1024])},
                {"new HexDumpBytes()", (Supplier<Bytes<?>>) HexDumpBytes::new},
        }));
        if (Jvm.maxDirectMemory()>0 ) {
            tests.addAll(Arrays.asList(new Object[][]{
                    {"Bytes.elasticByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260)},
                    {"Bytes.elasticByteBuffer(260, 1025)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260, 1025)},
                    {"Bytes.allocateElasticDirect(260)", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260)},
                    {"Bytes.allocateElasticDirect(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260).unchecked(true)},
                    {"Bytes.wrapForRead(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(ByteBuffer.allocateDirect(260))},
                    {"Bytes.wrapForWrite(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(ByteBuffer.allocateDirect(260))},
                    {"MappedBytes.mappedBytes(64K)", (Supplier<Bytes<?>>) () -> {
                        try {
                            return MappedBytes.mappedBytes(TMP_FILE, 64 << 10);
                        } catch (FileNotFoundException e) {
                            throw Jvm.rethrow(e);
                        }
                    }}
            }));
        }
        return tests;
    }

    @AfterEach
    @Override
    public void afterChecks() {
        if (bytes != null)
            bytes.releaseLast();
        super.afterChecks();
        new File(TMP_FILE).deleteOnExit();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readPositionAt0(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        bytes = supplier.get();
        assertEquals(0L, bytes.readPosition());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writePositionAt0(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        if (forRead) return;
        bytes = supplier.get();
        assertEquals(0L, bytes.writePosition());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void isClear(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        bytes = supplier.get();
        assertTrue(bytes.isClear());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void byteOrder(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        bytes = supplier.get();
        assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeLimit(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        bytes = supplier.get();
        assertTrue(bytes.writeLimit() >= 260);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        if (forRead) return;
        bytes = supplier.get();

        assertEquals(0, bytes.writePosition());
        assertTrue(bytes.isClear());
        bytes.writeInt(42);
        assertEquals(42, bytes.readInt());
        assertFalse(bytes.isClear());
        bytes.clear();
        assertTrue(bytes.isClear());
    }

    private void doAppend(BiConsumer<Bytes, CharSequence> append) {
        if (forRead) return;
        bytes = supplier.get();
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
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void appendSubstring(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend(ByteStringAppender::append);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void appendBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.append(Bytes.from("[" + s + "]"), 1, s.length() + 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void appendStringBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.append("[" + s + "]", 1, s.length() + 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void append8bitSubstring(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend(ByteStringAppender::append8bit);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void append8bitString(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.append8bit(s.toString()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void append8bitFromBytes(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.append8bit(Bytes.from(s)));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void append8bitFromBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.append8bit(Bytes.from("[" + s + "]"), 1L, s.length() + 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void append8bitStringBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.append8bit("[" + s + "]", 1, s.length() + 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeSubstring(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend(ByteStringAppender::write);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeFromBytes(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write(Bytes.from(s)));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeByteArray(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write(s.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeFromBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write(Bytes.from("[" + s + "]"), 1L, s.length()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeFromBytes2(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write((CharSequence) Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void appendUtf8Substring(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend(ByteStringAppender::appendUtf8);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bitSubstring(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend(ByteStringAppender::write8bit);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bitSubstring2(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write8bit(s.toString()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bitSubstringBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write8bit(s, 0, s.length()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bitFromBytes(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write8bit(Bytes.from(s)));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bitFromBytesBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write8bit(Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bitStringBounded(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.write8bit("[" + s + "]", 1, s.length()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeUtf8Substring(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend(ByteStringAppender::writeUtf8);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeUtf8Substring2(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.writeUtf8(s.toString()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeUtf8FromBytes(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        doAppend((b, s) -> b.writeUtf8(Bytes.from(s)));
    }

    @MethodSource("data")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "{0}")
    public void toString8bit(String testName, Supplier<Bytes<?>> supplier) {
        initBytes3Test(testName, supplier);
        if (forRead) return;
        bytes = supplier.get();
        for (char ch = 0; ch < 256; ch++) {
            bytes.writeUnsignedByte(ch);
        }
        String s = bytes.toString();
        for (char ch = 0; ch < 256; ch++) {
            assertEquals(ch, s.charAt(ch), "Character mismatch at index " + ch + " in: " + s);
        }
        assertEquals(256, s.length(), "Expected 256 characters, but got: " + s.length());
    }
}
