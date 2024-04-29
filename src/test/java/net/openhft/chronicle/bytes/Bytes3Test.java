/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(Parameterized.class)
public class Bytes3Test extends BytesTestCommon {

    private static final String TMP_FILE = OS.getTarget() + "/Bytes3Test-deleteme";
    private final Supplier<Bytes<?>> supplier;
    private final boolean forRead;
    private Bytes<?> bytes;

    public Bytes3Test(String testName, Supplier<Bytes<?>> supplier) {
        this.supplier = supplier;
        this.forRead = testName.contains("ForRead");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Bytes::elasticHeapByteBuffer", (Supplier<Bytes<?>>) Bytes::elasticHeapByteBuffer},
                {"Bytes.elasticByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260)},
                {"Bytes.elasticByteBuffer(260, 1025)", (Supplier<Bytes<?>>) () -> Bytes.elasticByteBuffer(260, 1025)},
                {"Bytes.elasticHeapByteBuffer(260)", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260)},
                {"Bytes.elasticHeapByteBuffer(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.elasticHeapByteBuffer(260).unchecked(true)},
                {"Bytes.allocateElasticDirect(260)", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260)},
                {"Bytes.allocateElasticDirect(260).unchecked", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(260).unchecked(true)},
                {"Bytes::allocateElasticOnHeap", (Supplier<Bytes<?>>) Bytes::allocateElasticOnHeap},
                {"Bytes.wrapForRead(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(ByteBuffer.allocateDirect(260))},
                {"Bytes.wrapForWrite(ByteBuffer.allocateDirect(200))", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(ByteBuffer.allocateDirect(260))},
                {"Bytes.wrapForRead(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForRead(new byte[1024])},
                {"Bytes.wrapForWrite(new byte[1024])", (Supplier<Bytes<?>>) () -> Bytes.wrapForWrite(new byte[1024])},
                {"new HexDumpBytes()", (Supplier<Bytes<?>>) HexDumpBytes::new},
                {"MappedBytes.mappedBytes(64K)", (Supplier<Bytes<?>>) () -> {
                    try {
                        return MappedBytes.mappedBytes(TMP_FILE, 64 << 10);
                    } catch (FileNotFoundException e) {
                        throw Jvm.rethrow(e);
                    }
                }}
        });
    }

    @Override
    public void afterChecks() {
        if (bytes != null)
            bytes.releaseLast();
        super.afterChecks();
        new File(TMP_FILE).deleteOnExit();
    }

    @Test
    public void readPositionAt0() {
        bytes = supplier.get();
        assertEquals(0L, bytes.readPosition());
    }

    @Test
    public void writePositionAt0() {
        if (forRead) return;
        bytes = supplier.get();
        assertEquals(0L, bytes.writePosition());
    }

    @Test
    public void isClear() {
        bytes = supplier.get();
        assertTrue(bytes.isClear());
    }

    @Test
    public void byteOrder() {
        bytes = supplier.get();
        assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder());
    }

    @Test
    public void writeLimit() {
        bytes = supplier.get();
        assertTrue(bytes.writeLimit() >= 260);
    }

    @Test
    public void write() {
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

    @Test
    public void appendSubstring() {
        doAppend(ByteStringAppender::append);
    }

    @Test
    public void appendBytesBounded() {
        doAppend((b, s) -> b.append(Bytes.from("[" + s + "]"), 1, s.length() + 1));
    }

    @Test
    public void appendStringBounded() {
        doAppend((b, s) -> b.append("[" + s + "]", 1, s.length() + 1));
    }

    @Test
    public void append8bitSubstring() {
        doAppend(ByteStringAppender::append8bit);
    }

    @Test
    public void append8bitString() {
        doAppend((b, s) -> b.append8bit(s.toString()));
    }

    @Test
    public void append8bitFromBytes() {
        doAppend((b, s) -> b.append8bit(Bytes.from(s)));
    }


    @Test
    public void append8bitFromBytesBounded() {
        doAppend((b, s) -> b.append8bit(Bytes.from("[" + s + "]"), 1L, s.length() + 1));
    }

    @Test
    public void append8bitStringBounded() {
        doAppend((b, s) -> b.append8bit("[" + s + "]", 1, s.length() + 1));
    }

    @Test
    public void writeSubstring() {
        doAppend(ByteStringAppender::write);
    }

    @Test
    public void writeFromBytes() {
        doAppend((b, s) -> b.write(Bytes.from(s)));
    }

    @Test
    public void writeByteArray() {
        doAppend((b, s) -> b.write(s.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void writeFromBytesBounded() {
        doAppend((b, s) -> b.write(Bytes.from("[" + s + "]"), 1L, s.length()));
    }

    @Test
    public void writeFromBytes2() {
        doAppend((b, s) -> b.write((CharSequence) Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @Test
    public void appendUtf8Substring() {
        doAppend(ByteStringAppender::appendUtf8);
    }

    @Test
    public void write8bitSubstring() {
        doAppend(ByteStringAppender::write8bit);
    }

    @Test
    public void write8bitSubstring2() {
        doAppend((b, s) -> b.write8bit(s.toString()));
    }

    @Test
    public void write8bitSubstringBounded() {
        doAppend((b, s) -> b.write8bit(s, 0, s.length()));
    }

    @Test
    public void write8bitFromBytes() {
        doAppend((b, s) -> b.write8bit(Bytes.from(s)));
    }

    @Test
    public void write8bitFromBytesBounded() {
        doAppend((b, s) -> b.write8bit(Bytes.from("[" + s + "]"), 1, s.length()));
    }

    @Test
    public void write8bitStringBounded() {
        doAppend((b, s) -> b.write8bit("[" + s + "]", 1, s.length()));
    }

    @Test
    public void writeUtf8Substring() {
        doAppend(ByteStringAppender::writeUtf8);
    }


    @Test
    public void writeUtf8Substring2() {
        doAppend((b, s) -> b.writeUtf8(s.toString()));
    }

    @Test
    public void writeUtf8FromBytes() {
        doAppend((b, s) -> b.writeUtf8(Bytes.from(s)));
    }
}
